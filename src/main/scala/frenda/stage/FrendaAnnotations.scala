package frenda.stage

import firrtl.AnnotationSeq
import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.ir.Circuit
import firrtl.options.{HasShellOptions, ShellOption, Unserializable}

sealed trait FrendaAnnotation extends Unserializable {
  this: Annotation =>
}

case class JobsAnnotation(jobs: Int)
  extends NoTargetAnnotation
    with FrendaAnnotation

object JobsAnnotation extends HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Int](
      longOption = "jobs",
      shortOption = Some("j"),
      toAnnotationSeq = (i: Int) => Seq(JobsAnnotation(i)),
      helpText = "specifies the number of jobs to run simultaneously, default to 1",
    )
  )
}

case object SilentModeAnnotation
  extends NoTargetAnnotation
    with FrendaAnnotation
    with HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Unit](
      longOption = "silent-mode",
      shortOption = Some("s"),
      toAnnotationSeq = _ => Seq(SilentModeAnnotation),
      helpText = "do not display any additional information on the screen",
    )
  )
}

final case class FrendaOptions(targetDir: String, jobs: Int, silentMode: Boolean) {
  /**
   * Logs message if not in silent mode.
   *
   * @param message the message
   */
  @inline def log(message: String): Unit = if (!silentMode) System.err.println(message)

  /**
   * Logs message if not in silent mode (thread-safe).
   *
   * @param message the message
   */
  @inline def logSync(message: String): Unit = if (!silentMode) System.err.synchronized {
    System.err.println(message)
  }
}

object FrendaOptions {
  /**
   * Gets `FrendaOptions` from annotations.
   *
   * @param annotations the sequence of annotations
   * @return options
   */
  def fromAnnotations(annotations: AnnotationSeq): FrendaOptions =
    annotations.collectFirst { case FrendaOptionsAnnotation(o) => o }.get
}

case class FrendaOptionsAnnotation(frendaOptions: FrendaOptions)
  extends NoTargetAnnotation
    with FrendaAnnotation

final case class SplitModule(name: String, circuit: Circuit)

case class SplitModulesAnnotation(modules: Seq[SplitModule])
  extends NoTargetAnnotation
    with FrendaAnnotation
