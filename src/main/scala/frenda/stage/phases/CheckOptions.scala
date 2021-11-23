package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Phase, TargetDirAnnotation}
import firrtl.stage.{FirrtlCircuitAnnotation, FirrtlFileAnnotation, FirrtlSourceAnnotation}
import frenda.FrendaException
import frenda.stage._

/**
 * Checks command line options, including input and output.
 */
class CheckOptions extends Phase {
  override def prerequisites = Seq()

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

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
    }
    val targetDir = annotations collectFirst { case TargetDirAnnotation(s) => s } getOrElse "."
    // check Frenda related options
    val jobs = annotations collectFirst { case JobsAnnotation(i) => i } getOrElse 1
    if (jobs < 1) {
      throw new FrendaException(s"Error: jobs number must be greater than 0, but found $jobs")
    }
    val outputDotF = annotations.collectFirst { case OutputDotFFileAnnotation(s) => s }
    val silentMode = annotations.exists { case SilentModeAnnotation => true; case _ => false }
    val cleanBuild = annotations.exists { case CleanBuildAnnotation => true; case _ => false }
    annotations ++ Seq(
      FrendaOptionsAnnotation(FrendaOptions(targetDir, jobs, outputDotF, silentMode, cleanBuild))
    )
  }
}
