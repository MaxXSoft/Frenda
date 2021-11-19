package frenda.stage.phases

import firrtl.options.Phase
import firrtl.stage.transforms.Compiler
import firrtl.stage.{FirrtlCircuitAnnotation, Forms}
import firrtl.{AnnotationSeq, CircuitState}

/**
 * Run some necessary cross-module transforms for the input FIRRTL.
 */
class PreTransform extends Phase {
  private val targets = Forms.HighForm

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.map {
    case FirrtlCircuitAnnotation(circuit) =>
      val compiler = new Compiler(targets, currentState = Nil)
      val transforms = compiler.flattenedTransformOrder
      val state = CircuitState(circuit, annotations)
      val newState = transforms.foldLeft(state) {
        case (prev, transform) => transform.runTransform(prev)
      }
      FirrtlCircuitAnnotation(newState.circuit)
    case other => other
  }
}
