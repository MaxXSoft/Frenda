package frenda

import firrtl.AnnotationSeq
import firrtl.options.StageMain
import frenda.stage.FrendaStage

/**
 * FIRRTL compiler of Frenda.
 *
 * @param annotations the input annotation sequence from command line
 */
private class FirrtlCompiler(override val annotations: AnnotationSeq)
  extends HasFrendaOptions {

  /**
   * Compiles the input FIRRTL incrementally.
   */
  def compile(): Unit = {
    // TODO
  }
}

/**
 * Entry point of Frenda.
 */
object FirrtlCompiler extends StageMain(new FrendaStage)
