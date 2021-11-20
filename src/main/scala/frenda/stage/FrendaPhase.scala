package frenda.stage

import firrtl.options.phases.WriteOutputAnnotations
import firrtl.options.{Dependency, PhaseManager}
import frenda.stage.phases._

/**
 * All phases required by Frenda.
 */
class FrendaPhase extends PhaseManager(FrendaPhase.targets)

object FrendaPhase {
  val targets: Seq[PhaseManager.PhaseDependency] = Seq(
    Dependency[CheckOptions],
    Dependency[AddCircuit],
    Dependency[PreTransform],
    Dependency[SplitCircuit],
    Dependency[IncrementalCompile],
    Dependency[WriteOutputAnnotations],
  )
}
