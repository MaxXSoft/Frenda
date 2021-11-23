package frenda.stage.phases

import firrtl.AnnotationSeq
import firrtl.options.{Dependency, Phase}
import frenda.stage.{FrendaOptions, WriteDotFFileAnnotation}

import java.nio.file.{Files, Paths}

/**
 * Write paths of generated Verilog file to the output dot f file.
 */
class WriteDotFFile extends Phase {
  override def prerequisites = Seq(Dependency[IncrementalCompile])

  override def optionalPrerequisites = Seq()

  override def optionalPrerequisiteOf = Seq()

  override def invalidates(a: Phase) = false

  override def transform(annotations: AnnotationSeq): AnnotationSeq = annotations.flatMap {
    case WriteDotFFileAnnotation(files) =>
      val options = FrendaOptions.fromAnnotations(annotations)
      options.outputDotF.foreach { path =>
        Files.writeString(Paths.get(path), files.mkString("\n"))
      }
      None
    case other => Some(other)
  }
}
