package worker

import akka.actor.{Actor, ActorSystem, Props}
import lib.Logger
import worker.{ TempInput, ExternalTempInput, InternalTempInput }
import mongodb.Upsert
import org.mongodb.scala.{MongoClient, MongoDatabase, MongoCollection, Document}
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant

case class Message(tempInput: TempInput)

class Collector() extends Actor {

  val mongoDbUrl = sys.env.getOrElse("MONGODB_URL", "mongodb://localhost:27017")
  val mongoDbDatabase = sys.env.getOrElse("MONGODB_DATABASE", "metrics")
  val mongoDbCollection = sys.env.getOrElse("MONGODB_COLLECTION", "temperature")

  /**
    * Convert converts input unix timestamp to a hourly timestamp
    * From: 1612350828 (2021-02-03T11:13:48.204018)
    * To: 1612350000 (2021-02-03T11:13:48)
    *
    * @param unixTimestamp ex: 1612350828
    * @return ex: 1612350000
    */
  def epochSecondsToTimeKey(unixTimestamp: Long): Long = {
    // fetch temp date
    // val dateTimeNow = LocalDateTime.now()
    val dateTimeNow = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneOffset.UTC)
    // fetch date parts
    val fetchYear = dateTimeNow.getYear()
    val fetchMonth = dateTimeNow.getMonthValue()
    val fetchDay = dateTimeNow.getDayOfMonth()
    val fetchHour = dateTimeNow.getHour()

    return LocalDateTime.of(fetchYear, fetchMonth, fetchDay, fetchHour, 0).toEpochSecond(ZoneOffset.UTC)
  }

  /** 
    * Upsert external & internal measurements to mongodb
    *
    * @param collection mongodb collection
    * @param ti internal or external temperature input
    */
  def upsert(collection: MongoCollection[Document], ti: TempInput): Unit = ti match {
    case ExternalTempInput(temperature, timestamp) => {
      try {
        // upsert external temperature
        Upsert.externalTemp(collection, epochSecondsToTimeKey(timestamp), timestamp, temperature)
      } catch {
        case t: Throwable => Logger.error(s"usert external temp record fail with error: ${t.getMessage()}")
      }
    }
    case InternalTempInput(id, temperature, humidity, timestamp) => {
      try {
        // upsert internal temperature metrics
        Upsert.internalTemp(collection, epochSecondsToTimeKey(timestamp), id, timestamp, temperature, humidity)
      } catch {
        case t: Throwable => Logger.error(s"usert internal temp record fail with error: ${t.getMessage()}")
      }

    }
  }

  /** Measurements receiver actor */
  def receive: Actor.Receive = {
    case Message(tempInput) => {
      try {
        // create new connection
        val client: MongoClient = MongoClient(mongoDbUrl)
        // select database > collection
        val db: MongoDatabase = client.getDatabase(mongoDbDatabase)
        val collection: MongoCollection[Document] = db.getCollection(mongoDbCollection)
        
        Logger.debug(s"upsert measurement: ${tempInput.toString()}")
        upsert(collection, tempInput)

        // close mongodb connection
        client.close()
      } catch {
        case t: Throwable => Logger.error(s"fail to upsert measurement ${tempInput.toString()} with error: ${t.getMessage()}")
      }
    }
  }
}
