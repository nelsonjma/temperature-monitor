package worker

import spray.json._


abstract class TempInput {
  def temperature: Float
  def timestamp: Long
}

// external temperature from near station, url: http://www.meteooeiras.com/dados-detalhados
case class ExternalTempInput(temperature: Float, timestamp: Long) extends TempInput
// internal temperatures from py sensors
case class InternalTempInput(id: String, temperature: Float, humidity: Int, timestamp: Long) extends TempInput

// connect case classes to json format, so that case classes can be serialized
trait InputsJsonProtocol extends DefaultJsonProtocol {

  implicit val externalTempFormat = jsonFormat2(ExternalTempInput)
  implicit val internalTempFormat = jsonFormat4(InternalTempInput)
}