package frenda.stage

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

case object StoreHashAnnotation
  extends NoTargetAnnotation
    with FrendaAnnotation
    with HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Unit](
      longOption = "store-hash",
      shortOption = Some("h"),
      toAnnotationSeq = _ => Seq(StoreHashAnnotation),
      helpText = "stores only hash instead of the full FIRRTL file",
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

case class SplitModulesAnnotations(circuits: Seq[Circuit])
  extends NoTargetAnnotation
    with FrendaAnnotation
