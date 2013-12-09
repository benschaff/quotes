package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Symbol(symbol: String, name: String)

case class Quote(symbol: Symbol, price: Double, delta: Double)

case class ChartRequestElement(symbol: String, value: String = "price", params: List[String] = List("c"))

case class ChartRequest(
  normalized: Boolean = false,
  numberOfDays: Int = 30,
  dataPeriod: String = "Day",
  elements: List[ChartRequestElement] = List()
)

object StockAPI {

  implicit val symbolReads: Reads[Symbol] = (
    (__ \ "Symbol").read[String] and
      (__ \ "Name").read[String]
    )(Symbol)

  implicit val symbolWrites: Writes[Symbol] = (
    (__ \ "symbol").write[String] and
      (__ \ "name").write[String]
    )(unlift(Symbol.unapply))

  implicit val quoteReads: Reads[Quote] = (
    ((__ \ "Symbol").read[String] and
      (__ \ "Name").read[String])(Symbol) and
      (__ \ "LastPrice").read[Double] and
      (__ \ "ChangePercent").read[Double]
    )(Quote)

  implicit val quoteWrites: Writes[Quote] = (
    ((__ \ "symbol").write[String] and
      (__ \ "name").write[String])(unlift(Symbol.unapply)) and
      (__ \ "price").write[Double] and
      (__ \ "delta").write[Double]
    )(unlift(Quote.unapply))

  implicit val chartRequestElementWrites: Writes[ChartRequestElement] = (
    (__ \ "Symbol").write[String] and
      (__ \ "Type").write[String] and
      (__ \ "Params").write[List[String]]
    )(unlift(ChartRequestElement.unapply))

  implicit val chartRequestWrites: Writes[ChartRequest] = (
    (__ \ "Normalized").write[Boolean] and
      (__ \ "NumberOfDays").write[Int] and
      (__ \ "DataPeriod").write[String] and
      (__ \ "Elements").write[List[ChartRequestElement]]
    )(unlift(ChartRequest.unapply))

}