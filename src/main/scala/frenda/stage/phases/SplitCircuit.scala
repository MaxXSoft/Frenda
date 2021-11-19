package frenda.stage.phases

import firrtl.ir.{Circuit, DefInstance, DefModule, ExtModule, Module, Statement}
import firrtl.options.Phase
import firrtl.stage.FirrtlCircuitAnnotation
import firrtl.{AnnotationSeq, WDefInstanceConnector}
import frenda.FrendaException
import frenda.stage.SplitModulesAnnotations

import scala.collection.mutable.ArrayBuffer

/**
 * Split the input circuit into single-module circuits.
 */
class SplitCircuit extends Phase {
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
   * @return sequence of FIRRTL modules (circuits)
   */
  private def splitCircuitIntoModules(circuit: Circuit): Seq[Circuit] = {
    val modMap = circuit.modules.map(m => m.name -> m).toMap
    // turn each module into it's own circuit with it as the top and all instantiated modules as `ExtModules`
    circuit.modules.collect {
      case m: Module =>
        val instModules = collectInstantiatedModules(m, modMap)
        val extModules = instModules.map {
          case Module(info, name, ports, _) => ExtModule(info, name, ports, name, Seq.empty)
          case ext: ExtModule => ext
        }
        Circuit(m.info, extModules :+ m, m.name)
    }
  }

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.map {
    case FirrtlCircuitAnnotation(circuit) =>
      SplitModulesAnnotations(splitCircuitIntoModules(circuit))
    case other => other
  }
}