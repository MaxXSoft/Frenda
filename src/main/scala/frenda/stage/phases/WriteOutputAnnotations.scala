package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Dependency, Phase, phases}

class WriteOutputAnnotations extends Phase {
  override def prerequisites = Seq(Dependency[IncrementalCompile])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq =
    new phases.WriteOutputAnnotations().transform(annotations)
}
