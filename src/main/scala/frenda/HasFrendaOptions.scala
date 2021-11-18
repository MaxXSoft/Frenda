package frenda

import firrtl.AnnotationSeq
import frenda.stage.{JobsAnnotation, SilentModeAnnotation, StoreHashAnnotation}

/**
 * Frenda command line argument parser.
 */
trait HasFrendaOptions {
  val annotations: AnnotationSeq

  val jobs: Int = annotations.collectFirst { case JobsAnnotation(i) => i } getOrElse 1
  val storeHash: Boolean = annotations.exists { case StoreHashAnnotation => true; case _ => false }
  val silentMode: Boolean = annotations.exists { case SilentModeAnnotation => true; case _ => false }
}
