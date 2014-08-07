import com.linkedin.context.DataHolderContextPropagator
import play.api.GlobalSettings
import play.api.libs.concurrent.ContextPropagator

/**
 * @author bbarkley
 */
object Global extends GlobalSettings {

  override def buildContextPropagator: Option[ContextPropagator] = Option(new DataHolderContextPropagator)
}

//trait Holder[T, S <: Holder[_, _]] {
//
//  def copy: S
//}
//
//class MyHolder extends Holder[String] {
//  override def copy: String = "2"
//}

//trait AContext
//
//trait AContextHolder[T <: AContext] {
//  def getContext: T
//  def setContext(c: T): Unit
//  def copy: AContextHolder[T]
//}
//
//class FooContext extends AContext
//
//class FooContextHolder extends AContextHolder[FooContext] {
//  override def getContext = new FooContext
//  override def setContext(c: FooContext) = println("got it")
//  override def copy = new FooContextHolder
//}
//
//
//trait Testing {
//  def buildContext: AContextHolder[AContext]
//}
//
//class MyTesting extends Testing {
//  override def buildContext: AContextHolder[AContext] = new FooContextHolder()
//}
//
//class More {
//  val testing = new MyTesting()
//  val built = testing.buildContext
//  val ctx = built.getContext
//  built.setContext(ctx)
//
//}