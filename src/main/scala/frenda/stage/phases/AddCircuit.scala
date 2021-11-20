package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Dependency, Phase}
import firrtl.stage.phases
import firrtl.stage.phases.AddDefaults
import frenda.stage.FrendaOptions

class AddCircuit extends Phase {
  override def prerequisites = Seq(Dependency[CheckOptions], Dependency[AddDefaults])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq = {
    val options = FrendaOptions.fromAnnotations(annotations)
    options.log("Parsing input circuit...")
    new phases.AddCircuit().transform(annotations)
  }
}
