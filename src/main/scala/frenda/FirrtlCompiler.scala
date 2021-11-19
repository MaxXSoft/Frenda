package frenda

import firrtl.options.StageMain
import frenda.stage.FrendaStage

/**
 * Entry point of Frenda.
 */
object FirrtlCompiler extends StageMain(new FrendaStage)
