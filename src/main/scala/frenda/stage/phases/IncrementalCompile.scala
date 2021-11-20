package frenda.stage.phases

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.{Input, Output}
import firrtl.ir.{DefModule, HashCode, StructuralHash}
import firrtl.options.{Dependency, Phase}
import firrtl.{AnnotationSeq, CircuitState, VerilogEmitter}
import frenda.stage.{FrendaOptions, FutureSplitModulesAnnotation, SplitModule}

import java.io.StringWriter
import java.nio.file.{Files, Path, Paths}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
 * Compiles split modules incrementally.
 */
class IncrementalCompile extends Phase {
  override def prerequisites = Seq(Dependency[SplitCircuit])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  /**
   * Gets a new Kryo instance.
   *
   * @return created Kryo instance
   */
  private def kryo(): Kryo = {
    val kryo = new Kryo
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
   * If so, returns a function for updating the hash file of the module.
   *
   * @param splitModule the input module
   * @param targetDir   path to target directory
   * @return some function for updating the hash file if the module should be compiled,
   *         otherwise `None`
   */
  private def shouldBeCompiled(splitModule: SplitModule, targetDir: String): Option[() => Unit] = {
    // get the hash code of the current `DefModule`
    val module = splitModule.circuit.modules.collectFirst { case m: DefModule => m }.get
    val hash = StructuralHash.sha256WithSignificantPortNames(module)
    // get the hash file of the current module
    val hashFile = Paths.get(targetDir, s"${splitModule.name}.hash")
    if (Files.notExists(hashFile)) {
      // file not found, compile for the first time
      return Some(() => updateHashFile(hashFile, hash))
    }
    // check the hash code
    val input = new Input(Files.newInputStream(hashFile))
    val result = kryo().readObject(input, hash.getClass) != hash
    input.close()
    // if re-compilation required, update the hash file
    Option.when(result) { () => updateHashFile(hashFile, hash) }
  }

  /**
   * Compiles the specific module to Verilog.
   *
   * @param options     Frenda related options
   * @param annotations sequence of annotations
   * @param splitModule the input module
   */
  private def compile(options: FrendaOptions,
                      annotations: AnnotationSeq,
                      splitModule: SplitModule): Unit = {
    // check if need to be compiled
    shouldBeCompiled(splitModule, options.targetDir) match {
      case Some(updateHash) =>
        // create a new verilog emitter with custom transforms
        val v = new VerilogEmitter
        // emit the current circuit
        val state = CircuitState(splitModule.circuit, annotations)
        val writer = new StringWriter
        v.emit(state, writer)
        // generate output
        val value = writer.toString.replaceAll("""(?m) +$""", "")
        options.logProgress(s"Done compiling module '${splitModule.name}'")
        // write to file
        val path = Paths.get(options.targetDir, s"${splitModule.name}.v")
        Files.writeString(path, value)
        // update the hash file
        updateHash()
      case None =>
        options.logProgress(s"Skipping module '${splitModule.name}'")
    }
  }

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.flatMap {
    case FutureSplitModulesAnnotation(modules) =>
      val options = FrendaOptions.fromAnnotations(annotations)
      // create compilation tasks
      implicit val ec: ExecutionContext = options.executionContext
      val tasks = modules.map { f => f.map(compile(options, annotations, _)) }
      // compile and get result
      options.log(s"Compiling ${options.totalProgress} modules...")
      Await.result(Future.sequence(tasks), Duration.Inf)
      None
    case other => Some(other)
  }
}
