package actors

import akka.actor._
import play.api.libs.concurrent.Akka
import play.api.Play.current
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws.WS
import models.Quote
import models.StockAPI._
import controllers.StockController
import scala.util.{Failure, Success}
import QuoteActor._
import akka.pattern.pipe
import scala.concurrent.Future

class QuoteActor(val symbol: String) extends Actor {

  private var cancel: Option[Cancellable] = None

  private var quote: Option[Quote] = None

  def receive = {
    case StartMessage =>
      if (!cancel.isDefined) {
        cancel = Some(Akka.system.scheduler.schedule(0.microsecond, 1.minute, self, RefreshMessage))
      }

    case StopMessage =>
      if (cancel.isDefined && !cancel.get.isCancelled) {
        cancel.get.cancel()

        cancel = None
      }

    case RefreshMessage => refresh()
    case QuoteActor.QuoteMessage => quotation pipeTo sender
  }

  def refresh() = {
    WS.url(StockController.quoteApiUrl).withQueryString("symbol" -> symbol).get map { response =>
      response.json.as[Quote]
    } onComplete {
      case Success(value) =>
        this.quote = Some(value)
        this.context.actorSelection(self.path.parent) ! QuoteRefreshMessage(symbol, value)

      case Failure(_) => this.quote = None
    }
  }

  def quotation: Future[Quote] = {
    if (quote.isDefined) Future { quote.get }
    else
      WS.url(StockController.quoteApiUrl).withQueryString("symbol" -> symbol).get map { response =>
        response.json.as[Quote]
      }
  }

  override def preStart() = {
    this.context.actorSelection(self.path.parent) ! QuoteActorReady(symbol, self)
  }

  override def postStop() = {
    if (cancel.isDefined && !cancel.get.isCancelled) {
      cancel.get.cancel()

      cancel = None

      this.context.actorSelection(self.path.parent) ! RestartQuoteActorWithRefresh
    }
  }

}

object QuoteActor {

  val StartMessage = "start"

  val StopMessage = "stop"

  val RefreshMessage = "refresh"

  val QuoteMessage = "quote"

  def create(symbol: String)(implicit context: ActorContext): ActorRef = {
    val symbolActor = context.actorOf(props(symbol), symbol)

    symbolActor
  }

  def props(symbol: String): Props = {
    Props(classOf[QuoteActor], symbol)
  }

}