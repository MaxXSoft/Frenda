package frenda

import firrtl.options.StageMain
import frenda.stage.FrendaStage

object FirrtlCompiler extends StageMain(new FrendaStage)
