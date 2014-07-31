package play.api.libs.concurrent

import java.util.concurrent._

import akka.dispatch.{ExecutorServiceFactory, ForkJoinExecutorConfigurator, ExecutorServiceConfigurator, DispatcherPrerequisites}
import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContextExecutor, CanAwait, ExecutionContext, Future}
import scala.util.Try

/**
 * @author bbarkley
 */


// TODO - may need to make this stateful for IC and add more lifecycle methods for 2-way prop
trait ContextPropagator {
  // TODO - figure out how to get this working with generics instead of a map
  def snapshotContext: Map[String, Any]

  def restoreContext(context: Map[String, Any])

  def clearContext()

  // TODO - would like this to return same type as itself - seems to require self referencing generics which got messy - http://stackoverflow.com/a/20165721/2175505
  def withInitialContext(context: Map[String, Any] = snapshotContext): ContextPropagator
}

class ContextPropagatingForkJoinExecutorServiceConfigurator(config: Config, prerequisites: DispatcherPrerequisites) extends ExecutorServiceConfigurator(config, prerequisites) {
  val forkJoinConfigurator = new ForkJoinExecutorConfigurator(config.getConfig("fork-join-executor"), prerequisites)

  override def createExecutorServiceFactory(id: String, threadFactory: ThreadFactory): ExecutorServiceFactory = {
    val ret = forkJoinConfigurator.createExecutorServiceFactory(id, threadFactory)
    new ContextPropagatingExecutorServiceFactory(ret)
  }

  class ContextPropagatingExecutorServiceFactory(delegate: ExecutorServiceFactory) extends ExecutorServiceFactory {
    override def createExecutorService: ExecutorService = {
      val contextPropagator = play.api.Play.maybeApplication.flatMap(_.global.buildContextPropagator)
      println("Context propagator from global is " + contextPropagator)
      contextPropagator.map{ propagator => new ContextPropagatingExecutorService(propagator, delegate.createExecutorService) }.getOrElse(delegate.createExecutorService)
    }
  }
}

class ContextPropagatingExecutorService(propagator: ContextPropagator, delegate: ExecutorService) extends AbstractExecutorService {
  override def awaitTermination(timeout: Long, unit: TimeUnit) = delegate.awaitTermination(timeout, unit)

  override def isShutdown() = delegate.isShutdown()

  override def isTerminated() = delegate.isTerminated()

  override def shutdown() = delegate.shutdown()

  override def shutdownNow() = delegate.shutdownNow()

  override def execute(command: Runnable) = {
    delegate.execute(new ContextPropagatingRunnable(propagator, command))
  }

  override def newTaskFor[T](callable: Callable[T]) = ContextPropagatingTask(propagator, callable)

  override def newTaskFor[T](runnable: Runnable, value: T) = ContextPropagatingTask(propagator, runnable, value)

  class ContextPropagatingTask[T](propagator: ContextPropagator, callable: Callable[T]) extends FutureTask[T](callable) {
    val incomingContext = propagator.snapshotContext
    val incomingThread = Thread.currentThread().getName
    override def run(): Unit = {
      var runLog = s"Starting run on thread ${Thread.currentThread().getName} with value of $incomingContext from thread $incomingThread"
      propagator.restoreContext(incomingContext)
      try {
        super.run()
      } finally {
        runLog += s"\nRun ended with ${propagator.snapshotContext}"
        println("====" + "\n" + runLog + "\n====\n\n")
        propagator.clearContext
      }
    }
  }

  object ContextPropagatingTask {
    def apply[T](propagator: ContextPropagator, callable: Callable[T]) = new ContextPropagatingTask[T](propagator, callable)
    def apply[T](propagator: ContextPropagator, runnable: Runnable, result: T) = new ContextPropagatingTask[T](propagator, Executors.callable(runnable, result))
  }

}


class ContextPropagatingFuture[T](wrapped: Future[T]) extends Future[T] {
  override def onComplete[U](func: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = wrapped.onComplete(func)(new WrappingExecutionContext(executor))

  override def isCompleted: Boolean = wrapped.isCompleted

  override def value: Option[Try[T]] = wrapped.value

  override def result(atMost: Duration)(implicit permit: CanAwait): T = wrapped.result(atMost)

  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    wrapped.ready(atMost)(permit)
    this
  }

}


class WrappingExecutionContext(delegate: ExecutionContext) extends ExecutionContextExecutor {
  val propagator: Option[ContextPropagator] = play.api.Play.maybeApplication.flatMap(_.global.buildContextPropagator).map(_.withInitialContext())
  override def reportFailure(t: Throwable): Unit = delegate.reportFailure(t)

  override def execute(command: Runnable): Unit = {
    val toRun: Runnable = propagator.map{ propagator =>
      new ContextPropagatingRunnable(propagator, command)
    }.getOrElse(command)
    delegate.execute(toRun)
  }
}


class ContextPropagatingRunnable(propagator: ContextPropagator, delegate: Runnable, name: Option[String] = None) extends Runnable {
  val incomingContext = propagator.snapshotContext
  val incomingThread = Thread.currentThread().getName
  override def run(): Unit = {
    var runLog = s"In $name starting run on thread ${Thread.currentThread().getName} with value of $incomingContext from thread $incomingThread"
    propagator.restoreContext(incomingContext)
    try {
      delegate.run()
    } finally {
      runLog += s"\nRun ended with ${propagator.snapshotContext}"
      println("====" + "\n" + runLog + "\n====\n\n")
      propagator.clearContext
    }
  }
}
