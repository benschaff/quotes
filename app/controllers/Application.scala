package controllers

import java.util.UUID

import actors.QuoteManagingActor
import akka.util.Timeout
import models.User
import models.User._
import play.api.Play.current
import play.api.cache.Cache
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.libs.openid.OpenID
import play.api.mvc._
import play.api.{Logger, Routes}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection._
import reactivemongo.core.errors.DatabaseException
import utils.Constants._
import utils.md5

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object Application extends Controller with MongoController {

  implicit val timeout = Timeout(5.seconds)

  def signupPath(mail: String): String = s"/#/signup/$mail"

  def indexWithSSO(mail: String): String = s"/#/mail=$mail"

  def users: JSONCollection = db.collection[JSONCollection]("user")

  def index(mail: Option[String]) = Action.async {
    Future {
      Ok(views.html.index())
    }
  }

  def authenticate(mail: String, password: String): Action[AnyContent] =  Action.async {  request =>
    authenticate(request, mail, password, sso = false) {
      NotFound
    }
  }

  def authenticate(request: Request[AnyContent], mail: String, password: String, sso: Boolean, redirect: Option[Result] = None)
                  (notFound: => Result): Future[Result] = {

    def findUser = {
      users.find(Json.obj("mail" -> mail)).one[User] map {
        case Some(user) =>
          if (user.active && (user.password.equalsIgnoreCase(password) || sso)) {
            val sessionId = UUID.randomUUID().toString
            Cache.set(s"$sessionId.$CachedProfilePath", user)

            if (redirect.isDefined) redirect.get.withSession(request.session + (SessionIdKey, sessionId))
            else Ok(Json.toJson(user.view)).withSession(request.session + (SessionIdKey, sessionId))
          } else NotFound
        case None => notFound
      }
    }

    val sessionId = request.session.get(SessionIdKey)
    if (sessionId.isDefined) {
      val user = Cache.getAs[User](s"${sessionId.get}.$CachedProfilePath")
      if (user.isDefined && user.get.mail == mail) Future { Ok(Json.toJson(Cache.getAs[User](s"${sessionId.get}.$CachedProfilePath").get.view)) }
      else findUser
    } else {
      findUser
    }
  }

  def signUp(mail: String, password: String) = Action.async { request =>
    val user = User(mail, md5(password).toLowerCase, List(), readOnly = false, active = true)
    users.save(user) map { error =>
      val sessionId = UUID.randomUUID().toString
      Cache.set(s"$sessionId.$CachedProfilePath", user)

      Ok(Json.toJson(user.view)).withSession(request.session + (SessionIdKey, sessionId))
    } recover {
      case e: DatabaseException =>
        e.code.map {
          case 11000 => NotAcceptable
          case _ => InternalServerError
        }.get
      case e: Exception =>
        Logger.error("SignUp ended with exception = {}.", e)

        InternalServerError
    }
  }

  def signOff = Action.async { request => Future {
    val user = request.session.get(SessionIdKey) map { sessionId =>
      val user = Cache.getAs[User](s"$sessionId.$CachedProfilePath").get

      QuoteManagingActor.cancel(Cache.getAs[User](s"$sessionId.$CachedProfilePath").get)

      Cache.remove(s"$sessionId.$CachedProfilePath")

      request.session - SessionIdKey

      user
    }

    Ok
  }}

  def openIdLogin(openid: String) = Action.async { implicit request =>
    Form(single(
      "openid" -> nonEmptyText
    )).bindFromRequest.fold(
      error => {
        Logger.info("OpendId Login as issued a bad request." + error.toString)

        Future { BadRequest }
      },
      {
        case (openIdUrl) =>
          OpenID.redirectURL(openIdUrl, routes.Application.openIdValidation.absoluteURL(), Seq("email" -> "http://schema.openid.net/contact/email")).map {
            url => Redirect(url)
          } recover {
            case t: Throwable => Redirect(routes.Application.index(None))
          }
      }
    )
  }

  def openIdValidation = Action.async { implicit request =>
      OpenID.verifiedId flatMap {
        info =>
          authenticate(request, info.attributes("email"), "", sso = true, Some(Redirect(indexWithSSO(info.attributes("email"))))) {
            Redirect(signupPath(info.attributes("email")))
          }
      } recover {
        case t: Throwable =>
          Logger.info("OpendId Validation failed:" + t.getMessage)

          Redirect(routes.Application.index(None))
      }
  }

  def javascriptRoutes = Action { implicit request =>
    Ok {
      Routes.javascriptRouter("javascriptRoutes") (
        routes.javascript.Application.authenticate,
        routes.javascript.Application.signOff,
        routes.javascript.Application.signUp
      )
    }.as("text/javascript")
  }

}