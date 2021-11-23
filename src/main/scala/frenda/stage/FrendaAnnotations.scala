package frenda.stage

import firrtl.AnnotationSeq
import firrtl.annotations.{Annotation, NoTargetAnnotation}
import firrtl.ir.Circuit
import firrtl.options.{HasShellOptions, ShellOption, Unserializable}

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ExecutionContext, Future}

sealed trait FrendaAnnotation extends Unserializable {
  this: Annotation =>
}

case class JobsAnnotation(jobs: Int)
  extends NoTargetAnnotation
    with FrendaAnnotation

object JobsAnnotation extends HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Int](
      longOption = "jobs",
      shortOption = Some("j"),
      toAnnotationSeq = i => Seq(JobsAnnotation(i)),
      helpText = "The number of jobs to run simultaneously, default to 1",
    )
  )
}

case class OutputDotFFileAnnotation(file: String)
  extends NoTargetAnnotation
    with FrendaAnnotation

object OutputDotFFileAnnotation extends HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[String](
      longOption = "output-f-file",
      shortOption = Some("off"),
      toAnnotationSeq = s => Seq(OutputDotFFileAnnotation(s)),
      helpText = "The output '.f' file",
      helpValueName = Some("<file>"),
    )
  )
}

case object SilentModeAnnotation
  extends NoTargetAnnotation
    with FrendaAnnotation
    with HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Unit](
      longOption = "silent-mode",
      shortOption = Some("s"),
      toAnnotationSeq = _ => Seq(SilentModeAnnotation),
      helpText = "Do not display any additional information on the screen",
    )
  )
}

case object CleanBuildAnnotation
  extends NoTargetAnnotation
    with FrendaAnnotation
    with HasShellOptions {
  val options: Seq[ShellOption[_]] = Seq(
    new ShellOption[Unit](
      longOption = "clean-build",
      shortOption = Some("cb"),
      toAnnotationSeq = _ => Seq(CleanBuildAnnotation),
      helpText = "Ignores the build cache and perform a clean build",
    )
  )
}

final case class FrendaOptions(targetDir: String,
                               jobs: Int,
                               outputDotF: Option[String],
                               silentMode: Boolean,
                               cleanBuild: Boolean) {
  class GlobalExecutionContext extends ExecutionContext {
    private val threadPool = Executors.newFixedThreadPool(jobs)

    override def execute(runnable: Runnable): Unit = threadPool.submit(runnable)

    override def reportFailure(cause: Throwable): Unit = ()

    def shutdown(): Unit = threadPool.shutdown()
  }

  /** The global execution context of all `Future`s. */
  lazy val executionContext: GlobalExecutionContext = new GlobalExecutionContext

  /** Total progress. */
  var totalProgress: Int = 0

  /** Current progress. */
  private val currentProgress = new AtomicInteger

  /** The print stream of logger. */
  private val stream = System.out

  /**
   * Logs message if not in silent mode.
   *
   * @param message the message
   */
  @inline def log(message: String): Unit = if (!silentMode) stream.println(message)

  /**
   * Logs message if not in silent mode (thread-safe).
   *
   * @param message the message
   */
  @inline def logSync(message: String): Unit = if (!silentMode) stream.synchronized {
    stream.println(message)
  }

  /**
   * Logs message with progress information if not in silent mode (thread-safe).
   *
   * @param message the message
   */
  @inline def logProgress(message: String): Unit = if (!silentMode) {
    val progress = currentProgress.incrementAndGet()
    stream.synchronized {
      stream.println(s"[$progress/$totalProgress] $message")
    }
  }
}

object FrendaOptions {
  /**
   * Gets `FrendaOptions` from annotations.
   *
   * @param annotations the sequence of annotations
   * @return options
   */
  def fromAnnotations(annotations: AnnotationSeq): FrendaOptions =
    annotations.collectFirst { case FrendaOptionsAnnotation(o) => o }.get
}

case class FrendaOptionsAnnotation(frendaOptions: FrendaOptions)
  extends NoTargetAnnotation
    with FrendaAnnotation

final case class SplitModule(name: String, circuit: Circuit)

case class FutureSplitModulesAnnotation(futures: Seq[Future[SplitModule]])
  extends NoTargetAnnotation
    with FrendaAnnotation {
  override def toString: String = s"FutureSplitModulesAnnotation(${futures.length} x Future)"
}
