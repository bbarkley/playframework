package scala.concurrent

import scala.concurrent.duration.Duration
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * @author bbarkley
 */
class ContextPropagatingFuture[T](wrapped: Future[T]) extends Future[T] {
  override def onComplete[U](func: (Try[T]) => U)(implicit executor: ExecutionContext): Unit = wrapped.onComplete(func)(executor)

  override def isCompleted: Boolean = wrapped.isCompleted

  override def value: Option[Try[T]] = wrapped.value

  override def result(atMost: Duration)(implicit permit: CanAwait): T = wrapped.result(atMost)

  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    wrapped.ready(atMost)(permit)
    this
  }

  override def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S] = {
    val p = Promise[S]()
    onComplete {
      case f: Failure[_] => p complete f.asInstanceOf[Failure[S]]
      case Success(v) => try f(v) match {
        case fut => fut.onComplete(p.complete)(Future.InternalCallbackExecutor)
      } catch { case NonFatal(t) => p failure t }
    }
    p.future
  }

}