import actors.QuoteManagingActor
import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    QuoteManagingActor()
  }

}