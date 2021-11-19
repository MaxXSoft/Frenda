package frenda.stage

import firrtl.options.{Dependency, PhaseManager}
import firrtl.stage.phases.AddCircuit
import frenda.stage.phases.CheckOptions

/**
 * All phases required by Frenda.
 */
class FrendaPhase extends PhaseManager(FrendaPhase.targets)

object FrendaPhase {
  val targets: Seq[PhaseManager.PhaseDependency] = Seq(
    Dependency[CheckOptions],
    Dependency[AddCircuit],
    // TODO
  )
}
