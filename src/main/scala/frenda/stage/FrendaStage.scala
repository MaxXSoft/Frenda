package frenda.stage

import firrtl.AnnotationSeq
import firrtl.options.{Shell, Stage}
import firrtl.stage.FirrtlCli

class FrendaStage extends Stage {
  val shell: Shell = new Shell(applicationName = "frenda") with FrendaCli with FirrtlCli

  def run(annotations: AnnotationSeq): AnnotationSeq = {
    // TODO
    annotations
  }
}
