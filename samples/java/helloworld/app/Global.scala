import java.util.concurrent.ExecutorService

import com.linkedin.context.DataHolderContextPropagator
import play.api.GlobalSettings
import play.api.libs.concurrent.{ContextPropagator, ContextPropagatingExecutorService}

object Global extends GlobalSettings {

  override def getExecutorServiceDecorator(delegate: ExecutorService): Option[ExecutorService] = {
    Option(new ContextPropagatingExecutorService(new DataHolderContextPropagator, delegate))
    //    None
  }

  override def buildContextPropagator: Option[ContextPropagator] = Option(new DataHolderContextPropagator)
}