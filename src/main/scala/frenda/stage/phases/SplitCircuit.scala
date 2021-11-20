package frenda.stage.phases

import firrtl.ir.{Circuit, DefInstance, DefModule, ExtModule, Module, Statement}
import firrtl.options.{Dependency, Phase}
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.{AnnotationSeq, WDefInstanceConnector}
import frenda.FrendaException
import frenda.stage.{FrendaOptions, SplitModule, FutureSplitModulesAnnotation}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

/**
 * Split the input circuit into single-module circuits.
 */
class SplitCircuit extends Phase {
  override def prerequisites = Seq(Dependency[PreTransform])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  /**
   * Collects all instantiated modules in a module.
   *
   * @param mod the input module
   * @param map all defined modules in the circuit
   * @return sequence of defined modules
   */
  private def collectInstantiatedModules(mod: Module, map: Map[String, DefModule]): Seq[DefModule] = {
    // use list instead of set to maintain order
    val modules = ArrayBuffer.empty[DefModule]

    def onStmt(stmt: Statement): Unit = stmt match {
      case DefInstance(_, _, name, _) => modules += map(name)
      case _: WDefInstanceConnector => throw new FrendaException(s"unrecognized statement: $stmt")
      case other => other.foreachStmt(onStmt)
    }

    onStmt(mod.body)
    modules.distinct.toSeq
  }

  /**
   * Split the specific circuit to separated modules.
   *
   * @param circuit the input circuit
   * @return sequence of split FIRRTL modules (circuits)
   */
  private def splitCircuitIntoModules(options: FrendaOptions, circuit: Circuit): Seq[Future[SplitModule]] = {
    val modMap = circuit.modules.map(m => m.name -> m).toMap
    // turn each module into it's own circuit with it as the top and all instantiated modules as `ExtModules`
    circuit.modules.collect {
      case m: Module =>
        implicit val ec: ExecutionContext = options.executionContext
        Future {
          val instModules = collectInstantiatedModules(m, modMap)
          val extModules = instModules.map {
            case Module(info, name, ports, _) => ExtModule(info, name, ports, name, Seq.empty)
            case ext: ExtModule => ext
          }
          val circuit = Circuit(m.info, extModules :+ m, m.name)
          options.logProgress(s"Done splitting module '${m.name}'")
          SplitModule(m.name, circuit)
        }
    }
  }

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.map {
    case FirrtlCircuitAnnotation(circuit) =>
      val options = FrendaOptions.fromAnnotations(annotations)
      // create tasks for splitting modules
      options.log("Splitting circuit...")
      val tasks = splitCircuitIntoModules(options, circuit)
      // update total progress for splitting and compiling modules
      options.totalProgress = tasks.length * 2
      FutureSplitModulesAnnotation(tasks)
    case other => other
  }
}
