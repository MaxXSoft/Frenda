package frenda.stage

import firrtl.options.Shell

/**
 * The command line interface of Frenda.
 */
trait FrendaCli {
  this: Shell =>
  parser.note("Frenda Specific Options")
  Seq(
    JobsAnnotation,
    StoreHashAnnotation,
    SilentModeAnnotation,
  ).foreach(_.addOptions(parser))
}
