import com.linkedin.context.DataHolderContextPropagator
import play.api.GlobalSettings
import play.api.libs.concurrent.ContextPropagator

object Global extends GlobalSettings {

  override def buildContextPropagator: Option[ContextPropagator] = Option(new DataHolderContextPropagator)
}