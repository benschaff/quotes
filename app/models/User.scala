package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class User(mail: String, password: String, followedStocks: List[String], readOnly: Boolean, active: Boolean) {

  def view = UserView(mail, followedStocks, readOnly)

}

case class UserView(mail: String, followedStocks: List[String], readOnly: Boolean)

case class Company(mail: String, name: String, uuid: String)

object User {

  implicit val userViewWrites: Writes[UserView] = (
    (__ \ "mail").write[String] and
      (__ \ "followedStocks").write[List[String]] and
      (__ \ "readOnly").write[Boolean]
    )(unlift(UserView.unapply))

  implicit val companyFormat = Json.format[Company]

  implicit val userFormat = Json.format[User]

}
