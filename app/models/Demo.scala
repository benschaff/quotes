package models

import play.api.libs.json.Json
import StockAPI.symbolWrites
import StockAPI.symbolReads

case class Demo(user: User, symbols: List[Symbol])

object Demo {

  implicit val demoFormat = Json.format[Demo]

}
