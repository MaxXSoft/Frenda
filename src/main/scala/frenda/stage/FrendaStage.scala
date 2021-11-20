package frenda.stage

import firrtl.AnnotationSeq
import firrtl.options.{Shell, Stage}
import firrtl.stage.FirrtlCli

/**
 * Main stage of Frenda.
 *
 * The stage will read the input FIRRTL, and then compile it incrementally.
 */
class FrendaStage extends Stage {
  val shell: Shell = new Shell(applicationName = "frenda") with FrendaCli with FirrtlCli

  def run(annotations: AnnotationSeq): AnnotationSeq = {
    val result = new FrendaPhase().transform(annotations)
    FrendaOptions.fromAnnotations(result).executionContext.shutdown()
    result
  }
}
