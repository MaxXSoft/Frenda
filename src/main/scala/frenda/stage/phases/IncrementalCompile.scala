package frenda.stage.phases

import com.twitter.chill.{Input, KryoBase, Output, ScalaKryoInstantiator}
import firrtl.ir.{HashCode, Module, StructuralHash}
import firrtl.options.{Dependency, Phase}
import firrtl.stage.transforms.Compiler
import firrtl.{AnnotationSeq, CircuitState, EmitCircuitAnnotation, EmittedVerilogCircuitAnnotation, VerilogEmitter}
import frenda.stage.{FrendaOptions, FutureSplitModulesAnnotation, SplitModule, WriteDotFFileAnnotation}

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
  private def kryo(): KryoBase = {
    val inst = new ScalaKryoInstantiator
    inst.setRegistrationRequired(false)
    inst.newKryo()
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
   * @param options     Frenda related options
   * @param splitModule the input module
   * @return some function for updating the hash file if the module should be compiled,
   *         otherwise `None`
   */
  private def shouldBeCompiled(options: FrendaOptions,
                               splitModule: SplitModule): Option[() => Unit] = {
    // get the hash code of the current `Module`
    val module = splitModule.circuit.modules.collectFirst { case m: Module => m }.get
    val hash = StructuralHash.sha256WithSignificantPortNames(module)
    // get the hash file of the current module
    val hashFile = Paths.get(options.targetDir, s"${splitModule.name}.hash")
    if (options.cleanBuild || Files.notExists(hashFile)) {
      // clean build or hash file not found, just compile
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
   * @return path to generated verilog file and recompilation flag
   */
  private def compile(options: FrendaOptions,
                      annotations: AnnotationSeq,
                      splitModule: SplitModule): Option[(String, Boolean)] = {
    // check if need to be compiled
    shouldBeCompiled(options, splitModule) match {
      case Some(updateHash) =>
        // emit the current circuit
        val circuitAnnotations = annotations ++ Seq(
          EmitCircuitAnnotation(classOf[VerilogEmitter]),
        )
        val state = CircuitState(splitModule.circuit, circuitAnnotations)
        val compiler = new Compiler(Seq(Dependency[VerilogEmitter]), PreTransform.targets)
        val newState = compiler.transform(state)
        // generate output
        options.logProgress(s"Done compiling module '${splitModule.name}'")
        newState.annotations.collectFirst {
          case EmittedVerilogCircuitAnnotation(e) =>
            // write to Verilog file and hash file
            val path = Paths.get(options.targetDir, s"${splitModule.name}.v")
            Files.writeString(path, e.value)
            updateHash()
            (path.toRealPath().toAbsolutePath.toString, true)
        }
      case None =>
        options.logProgress(s"Skipping module '${splitModule.name}'")
        val path = Paths.get(options.targetDir, s"${splitModule.name}.v")
        Option.when(Files.exists(path)) {
          (path.toRealPath().toAbsolutePath.toString, false)
        }
    }
  }

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.flatMap {
    case FutureSplitModulesAnnotation(futures) =>
      val options = FrendaOptions.fromAnnotations(annotations)
      // create compilation tasks
      implicit val ec: ExecutionContext = options.executionContext
      val tasks = futures.map { f => f.map(compile(options, annotations, _)) }
      // compile and get results
      options.log(s"Compiling ${futures.length} modules...")
      val results = Await.result(Future.sequence(tasks), Duration.Inf).flatten
      options.log(s"Done compiling")
      // generate annotation if recompiled
      Option.when(results.exists(_._2)) {
        WriteDotFFileAnnotation(results.map(_._1))
      }
    case other => Some(other)
  }
}
