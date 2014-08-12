package play.api.libs.concurrent

import java.util.concurrent._

import akka.dispatch._
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * @author bbarkley
 */


// TODO - may need to make this stateful for IC and add more lifecycle methods for 2-way prop
trait ContextPropagator { self =>
  type Context = Option[Map[String, Any]]
  // TODO - figure out how to get this working with generics instead of a map
  // if the context doesn't exists (or is empty return None)
  def snapshotContext: Option[Map[String, Any]]

  def restoreContext(context: Map[String, Any])

  def clearContext()

  def wrapRunnable(callSiteCtx: Context, executeCtx: Context, runnable: Runnable): Runnable = {
    new Runnable {
      override def run(): Unit = {
        self.restoreContext(executeCtx.getOrElse(callSiteCtx.getOrElse(Map())))
        try {
          runnable.run()
        } finally {
          self.clearContext()
        }
      }
    }
  }

  // TODO - would like this to return same type as itself - seems to require self referencing generics which got messy - http://stackoverflow.com/a/20165721/2175505
  def withInitialContext(context: Map[String, Any] = snapshotContext.get): ContextPropagator
}

/**
 * Modeled after DispatcherConfigurator
 * @param config
 * @param prerequisites
 */
class ContextPropagatingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends MessageDispatcherConfigurator(config, prerequisites) {
  private val instance = new ContextPropagatingDispatcher(
    this,
    config.getString("id"),
    config.getInt("throughput"),
    Duration(config.getNanoseconds("throughput-deadline-time"), TimeUnit.NANOSECONDS),
    configureExecutor(),
    Duration(config.getMilliseconds("shutdown-timeout"), TimeUnit.MILLISECONDS))

  /**
   * Returns the same dispatcher instance for each invocation
   */
  override def dispatcher(): MessageDispatcher = instance
}

class ContextPropagatingDispatcher(_configurator: MessageDispatcherConfigurator,
                                   override val id: String,
                                   override val throughput: Int,
                                   override val throughputDeadlineTime: Duration,
                                   executorServiceFactoryProvider: ExecutorServiceFactoryProvider,
                                   override val shutdownTimeout: FiniteDuration)
  extends Dispatcher(_configurator, id, throughput, throughputDeadlineTime, executorServiceFactoryProvider, shutdownTimeout) { self =>

  override def prepare(): ExecutionContext = {
    val contextPropagator = play.api.Play.maybeApplication.flatMap(_.global.buildContextPropagator)
    val initialState = contextPropagator.flatMap(_.snapshotContext)
    val prepareThread = Thread.currentThread().getName
    new ExecutionContext {

      override def reportFailure(t: Throwable): Unit = self.reportFailure(t)

      override def execute(runnable: Runnable): Unit = {
        val incomingThreadState = contextPropagator.flatMap(_.snapshotContext)
        val incomingThread = Thread.currentThread().getName
        val wrappedRunnable = contextPropagator.map(_.wrapRunnable(initialState, incomingThreadState, runnable)).getOrElse(runnable)
        self.execute(new Runnable() {
          override def run(): Unit = {
            println(s"Running on thread ${Thread.currentThread().getName} original: ${initialState.orNull} from $prepareThread incoming: ${incomingThreadState.orNull} from $incomingThread")
            wrappedRunnable.run()
          }
        })
      }
    }
  }
}