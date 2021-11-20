package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Dependency, Phase}
import firrtl.stage.phases
import firrtl.stage.phases.AddDefaults

class AddCircuit extends Phase {
  override def prerequisites = Seq(Dependency[CheckOptions], Dependency[AddDefaults])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq =
    new phases.AddCircuit().transform(annotations)
}
