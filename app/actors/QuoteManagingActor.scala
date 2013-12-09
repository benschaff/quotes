package actors

import akka.actor.{Terminated, ActorRef, Props, Actor}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._
import models.{User, Quote}
import scala.collection.mutable
import actors.QuoteActor._
import play.api.libs.iteratee.Concurrent
import play.api.libs.json.{Json, JsValue}
import scala.concurrent.Future
import models.StockAPI._

class QuoteManagingActor extends Actor {

  private implicit val executionContext = this.context.dispatcher

  private implicit val actorContext = this.context

  private implicit val timeout = Timeout(5.seconds)

  private val toRestartWithRefresh: mutable.Set[String] = mutable.HashSet()

  private val liveUsers: mutable.Map[String, Concurrent.Channel[JsValue]] = mutable.HashMap()

  private val liveSymbols: mutable.Map[String, Set[String]] = mutable.HashMap()

  def receive = {
    case QuoteMessage(symbol) =>
      quoteActor[Future[Quote]](symbol) {
        val quoteActor = QuoteActor.create(symbol)
        actorContext.watch(quoteActor)

        (quoteActor ? QuoteActor.QuoteMessage).mapTo[Quote]
      } { quoteActor =>
        (quoteActor ? QuoteActor.QuoteMessage).mapTo[Quote]
      } pipeTo sender

    case RestartQuoteActorWithRefresh(symbol) => toRestartWithRefresh add symbol

    case QuoteActorReady(symbol, quoteActor) => if (toRestartWithRefresh contains symbol) {
      quoteActor ! StartMessage

      toRestartWithRefresh remove symbol
    }

    case QuoteRegistrationMessage(symbol) =>
      quoteActor[Unit](symbol) {
        val quoteActor = QuoteActor.create(symbol)
        actorContext.watch(quoteActor)

        quoteActor ! StartMessage
      } { _ ! StartMessage }

    case QuoteCancellationMessage(symbol) =>
      this.context.child(symbol).map { quoteActor =>
        quoteActor ! StopMessage
      }

    case QuoteRefreshMessage(symbol, quote) => liveSymbols(symbol).foreach(liveUsers(_).push(Json.toJson(quote)))

    case RegisterUserMessage(symbol, user, channel) =>
      if (!liveUsers.get(user.mail).isDefined) liveUsers.put(user.mail, channel)

      if (liveSymbols.contains(symbol)) liveSymbols.put(symbol, liveSymbols(symbol) + user.mail)
      else liveSymbols.put(symbol, Set(user.mail))

      self ! QuoteRegistrationMessage(symbol)

    case CancelUserMessage(symbol, user) =>
      if (liveSymbols.get(symbol).isDefined) liveSymbols.put(symbol, liveSymbols(symbol) - user.mail)
      if (liveSymbols(symbol).isEmpty) self ! QuoteCancellationMessage(symbol)
      if (!liveSymbols.exists(_._2.contains(user.mail))) liveUsers.remove(user.mail)

    case FollowSymbolMessage(symbol, user) =>
      if (liveUsers.get(user.mail).isDefined) self ! RegisterUserMessage(symbol, user, liveUsers(user.mail))

    case CancelSymbolMessage(symbol, user) => self ! CancelUserMessage(symbol, user)

    case Terminated(actor) => toRestartWithRefresh + actor.path.name
  }

  def quoteActor[T](symbol: String)(none: => T)(defined: (ActorRef) => T): T = {
    val quoteActor = this.context.child(symbol)
    if (!quoteActor.isDefined) {
      none
    } else {
      defined(quoteActor.get)
    }
  }

}

object QuoteManagingActor {

  val QuoteManagingActorName = "QuoteManagingActor"

  def apply() = {
    Akka.system.actorOf(props, QuoteManagingActorName)
  }

  def props: Props = {
    Props(classOf[QuoteManagingActor])
  }

  // TODO: Handle users with not symbols so they are registered
  def register(user: User, channel: Concurrent.Channel[JsValue]) = {
    user.followedStocks.foreach { symbol =>
      Akka.system.actorSelection(s"/user/${QuoteManagingActor.QuoteManagingActorName}") ! RegisterUserMessage(symbol, user, channel)
    }
  }

  def cancel(user: User) = {
    user.followedStocks.foreach { symbol =>
        Akka.system.actorSelection(s"/user/${QuoteManagingActor.QuoteManagingActorName}") ! CancelUserMessage(symbol, user)
    }
  }

  def follow(symbol: String, user: User) = {
    Akka.system.actorSelection(s"/user/${QuoteManagingActor.QuoteManagingActorName}") ! FollowSymbolMessage(symbol, user)
  }

  def cancel(symbol: String, user: User) = {
    Akka.system.actorSelection(s"/user/${QuoteManagingActor.QuoteManagingActorName}") ! CancelSymbolMessage(symbol, user)
  }

}

case class QuoteRegistrationMessage(symbol: String)
case class QuoteCancellationMessage(symbol: String)
case class QuoteMessage(symbol: String)
case class QuoteRefreshMessage(symbol: String, quote: Quote)

case class QuoteActorReady(symbol: String, quoteActorRef: ActorRef)
case class RestartQuoteActorWithRefresh(symbol: String)

case class RegisterUserMessage(symbol: String, user: User, channel: Concurrent.Channel[JsValue])
case class CancelUserMessage(symbol: String, user: User)

case class FollowSymbolMessage(symbol: String, user: User)
case class CancelSymbolMessage(symbol: String, user: User)