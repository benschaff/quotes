package controllers

import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, Json}
import models._
import play.api.libs.concurrent.Akka
import actors.QuoteManagingActor
import scala.concurrent.Future
import play.api.{Play, Routes, Logger}
import play.api.cache.Cache
import utils.Constants._
import play.api.libs.json.JsArray
import models.ChartRequestElement
import models.Symbol
import actors.QuoteMessage
import models.Quote
import models.ChartRequest
import play.api.libs.iteratee.Concurrent
import play.api.libs.EventSource
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import StockAPI._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

object StockController extends Controller with MongoController {

  implicit val timeout = Timeout(5.seconds)

  val symbolsApiUrl: String = Play.configuration.getString("markitondemand-api.lookup").get

  val chartDataApiUrl: String = Play.configuration.getString("markitondemand-api.chartData").get

  val quoteApiUrl: String = Play.configuration.getString("markitondemand-api.quote").get

  def users: JSONCollection = db.collection[JSONCollection]("user")

  def symbols(query: String) = Action.async {
    WS.url(symbolsApiUrl).withQueryString("input" -> query).get map { response =>
      Ok {
        Json.toJson(response.json.as[JsArray].value.map {
          value => value.as[Symbol]
        }.distinct)
      }
    }
  }

  def quotes(symbols: String) = Action.async {
    Akka.system.actorSelection(s"/user/${QuoteManagingActor.QuoteManagingActorName}").resolveOne()(timeout).flatMap { actor =>
      Future.sequence {
        symbols.split(',').toList.map { symbol =>
          (actor ? QuoteMessage(symbol)).mapTo[Quote]
        }
      } map { quotes =>
        Ok(Json.toJson(quotes))
      }
    } recover {
      case e: Exception =>
        Logger.error("Quote actor terminated with exception = {}.", e)

        InternalServerError
    }
  }

  def last30Days(symbol: String) = Action.async {
    val data = Cache.getAs[JsValue](s"$symbol.chartData.last30Days")
    if (data.isDefined) Future { Ok(data.get) }
    else
      WS.url(chartDataApiUrl).withQueryString {
        "parameters" -> Json.stringify(Json.toJson(ChartRequest(elements = List(ChartRequestElement(symbol = symbol)))))
      }.get map {
        response =>
          Cache.set(s"$symbol.chartData.last30Days", response.json, 24.hour)
          Ok(response.json)
      }
  }

  def follow(symbol: String) = Action.async { request => (
    request.session.get(SessionIdKey) map { sessionId =>
      WS.url(quoteApiUrl).withQueryString("symbol" -> symbol).get flatMap { response =>
        val user = Cache.getAs[User](s"$sessionId.$CachedProfilePath").get
        if (user.readOnly) Future { InternalServerError }
        else {
          val quote = response.json.as[Quote]

          val updatedUser = user.copy(followedStocks = user.followedStocks :+ quote.symbol.symbol)
          users.update(Json.obj("mail" -> updatedUser.mail), updatedUser) map { error =>
            Cache.set(s"$sessionId.$CachedProfilePath", updatedUser)

            QuoteManagingActor.follow(symbol, updatedUser)

            Ok(Json.toJson(quote))
          }
        }
      }
    }).get
  }

  def cancel(symbol: String) = Action.async { request => (
    request.session.get(SessionIdKey) map { sessionId =>
      val user = Cache.getAs[User](s"$sessionId.$CachedProfilePath").get
      if (user.readOnly) Future { InternalServerError }
      else {
        val updatedUser = user.copy(followedStocks = user.followedStocks.filter(_ != symbol))
        users.update(Json.obj("mail" -> updatedUser.mail), updatedUser) map { error =>
          Cache.set(s"$sessionId.$CachedProfilePath", updatedUser)

          QuoteManagingActor.cancel(symbol, updatedUser)

          Ok(Json.toJson(Symbol(symbol, "")))
        }
      }
    }).get
  }

  def quoteSource() = Action.async { request => (
    request.session.get(SessionIdKey) map { sessionId =>
      Future {
        val user = Cache.getAs[User](s"$sessionId.$CachedProfilePath").get

        val (out, channel) = Concurrent.broadcast[JsValue]

        QuoteManagingActor.register(user, channel)

        Ok.chunked(out through EventSource()).as("text/event-stream")
      }
    }).get
  }

  def demo = Action.async {
    if (Cache.get("demo").isDefined) Future { Ok(Json.toJson(Cache.getAs[Demo]("demo").get)) }
    else {
      users.find(Json.obj("mail" -> "demo@livequotes.com")).one[User] flatMap {
        case Some(demoUser) =>
          Future.sequence {
            demoUser.followedStocks map { symbol =>
              WS.url(symbolsApiUrl).withQueryString("input" -> symbol).get map {
                response => response.json.as[JsArray].value(0).as[Symbol]
              }
            }
          } map { symbols =>
            val demo = Demo(demoUser, symbols)

            Cache.set("demo", demo)

            Ok(Json.toJson(demo))
          }

        case None => Future {
          Logger.error("Failed to load demo user data")

          InternalServerError
        }
      }
    }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok {
      Routes.javascriptRouter("stockJavascriptRoutes") (
        routes.javascript.StockController.demo,
        routes.javascript.StockController.symbols,
        routes.javascript.StockController.last30Days,
        routes.javascript.StockController.quotes,
        routes.javascript.StockController.follow,
        routes.javascript.StockController.cancel,
        routes.javascript.StockController.quoteSource
      )
    }.as("text/javascript")
  }

}
