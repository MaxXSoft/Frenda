package frenda.stage.phases

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.{Input, Output}
import firrtl.ir.{DefModule, HashCode, StructuralHash}
import firrtl.options.{Phase, TargetDirAnnotation}
import firrtl.stage.{Forms, TransformManager}
import firrtl.{AnnotationSeq, CircuitState, EmittedVerilogModule, EmittedVerilogModuleAnnotation, Transform, VerilogEmitter}
import frenda.stage.{JobsAnnotation, SilentModeAnnotation, SplitModule, SplitModulesAnnotation}

import java.io.StringWriter
import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Compiles split modules incrementally.
 */
class IncrementalCompile extends Phase {
  require(Forms.VerilogOptimized.startsWith(Forms.HighForm))
  private val targets = Forms.VerilogOptimized.drop(Forms.HighForm.length)

  /**
   * Gets a new Kryo instance.
   *
   * @return created Kryo instance
   */
  private def kryo(): Kryo = {
    val kryo = new Kryo()
    kryo.setRegistrationRequired(false)
    kryo
  }

  /**
   * Updates the specific hash file by using the hash code object.
   *
   * @param path     the hash file
   * @param hashCode the hash code object
   */
  private def updateHashFile(path: Path, hashCode: HashCode): Unit = {
    val output = new Output(Files.newOutputStream(path))
    kryo().writeObject(output, hashCode)
    output.close()
  }

  /**
   * Checks if the specific module should be compiled.
   * If so, update the hash file of the module.
   *
   * @param splitModule the input module
   * @param targetDir   path to target directory
   * @return the module should be compiled
   */
  private def shouldBeCompiled(splitModule: SplitModule, targetDir: String): Boolean = {
    // get the hash code of the current `DefModule`
    val module = splitModule.circuit.modules.collectFirst { case m: DefModule => m }.get
    val hash = StructuralHash.sha256WithSignificantPortNames(module)
    // get the hash file of the current module
    val hashFile = Paths.get(targetDir, s"${splitModule.name}.hash")
    if (Files.notExists(hashFile)) {
      // file not found, create a new one
      updateHashFile(hashFile, hash)
      return true
    }
    // check the hash code
    val input = new Input(Files.newInputStream(hashFile))
    val result = kryo().readObject(input, hash.getClass) != hash
    input.close()
    // if re-compilation required, update the hash file
    if (result) updateHashFile(hashFile, hash)
    result
  }

  /**
   * Compiles the specific module to Verilog.
   *
   * @param annotations sequence of annotations
   * @param splitModule the input module
   * @param targetDir   path to target directory
   * @return compiled module, if it has already been compiled, returns `None`
   */
  private def compile(annotations: AnnotationSeq,
                      splitModule: SplitModule,
                      targetDir: String): Option[EmittedVerilogModule] = {
    if (!shouldBeCompiled(splitModule, targetDir)) return None
    // create a new verilog emitter with custom transforms
    val v = new VerilogEmitter {
      override def transforms: Seq[Transform] = {
        new TransformManager(targets).flattenedTransformOrder
      }
    }
    // emit the current circuit
    val state = CircuitState(splitModule.circuit, annotations)
    val writer = new StringWriter()
    v.emit(state, writer)
    // generate output
    val value = writer.toString.replaceAll("""(?m) +$""", "")
    Some(EmittedVerilogModule(splitModule.name, value, ".v"))
  }

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.flatMap {
    case SplitModulesAnnotation(modules) =>
      // get command line options
      val jobs = annotations.collectFirst { case JobsAnnotation(i) => i } getOrElse 1
      val targetDir = annotations.collectFirst { case TargetDirAnnotation(s) => s }.get
      // TODO: silence mode
      val silentMode = annotations.exists { case SilentModeAnnotation => true; case _ => false }

      // create a thread pool with `jobs` threads
      implicit val ec: ExecutionContext = new ExecutionContext {
        private val threadPool = Executors.newFixedThreadPool(jobs)

        override def execute(runnable: Runnable): Unit = threadPool.submit(runnable)

        override def reportFailure(cause: Throwable): Unit = ()
      }

      // compile and get result
      val tasks = modules.map { sm => Future(compile(annotations, sm, targetDir)) }
      Await.result(Future.sequence(tasks), Duration.Inf)
        .flatten
        .map(EmittedVerilogModuleAnnotation)

    case other => Seq(other)
  }
}
