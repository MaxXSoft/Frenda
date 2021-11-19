package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Phase, TargetDirAnnotation}
import firrtl.stage.{FirrtlCircuitAnnotation, FirrtlFileAnnotation, FirrtlSourceAnnotation}
import frenda.FrendaException

/**
 * Checks command line options, including input and output.
 */
class CheckOptions extends Phase {
  override def transform(annotations: AnnotationSeq): AnnotationSeq = {
    // check input
    val inputCount = annotations.count {
      case _: FirrtlFileAnnotation => true
      case _: FirrtlSourceAnnotation => true
      case _: FirrtlCircuitAnnotation => true
      case _ => false
    }
    if (inputCount > 1) {
      throw new FrendaException(s"Error: only one FIRRTL input should be present, but found $inputCount")
    } else if (inputCount < 1) {
      throw new FrendaException("Error: FIRRTL input not found, try -i <FIRRTL file>")
    }
    // check output
    val targetCount = annotations.count(_.isInstanceOf[TargetDirAnnotation])
    if (targetCount > 1) {
      throw new FrendaException(s"Error: there can only be 1 target directory, but found $targetCount")
    } else if (targetCount < 1) {
      throw new FrendaException("Error: target directory is not specified, try -td <directory>")
    }
    annotations
  }
}
