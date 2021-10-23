package mongodb

import lib.Logger
import mongodb.Helpers._

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.Instant

import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Projections

import org.mongodb.scala.{Observable, MongoCollection, Document, Completed}
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, set, push}
import org.mongodb.scala.SingleObservable
import org.mongodb.scala.bson.{BsonArray, BsonValue, BsonInt32}

import java.util.Date // necessary to create date columns


object Upsert {

  /**
    * Convert unix epoch seconds to java Date (AKA epoch milliseconds)
    *
    * @param unixTimestamp epoch seconds ex: 1612350000
    * @return java Date to be used in mongo ISODate
    */
  def epochSecondstoDate(unixTimestamp: Long): java.util.Date = {
    // convert seconds to datetime
    val dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneOffset.UTC)
    // convert datetime to epoch milliseconds
    val epochMillis =  dt.atZone(ZoneOffset.UTC).toInstant().toEpochMilli()

    return new java.util.Date(epochMillis)
  }

  /**
    * Calculate internal temperature/humidity average measurements
    *
    * @param collection mongodb collection
    * @param timeKey epoch timestamp ex: 1612350000 (2021-02-03T11:13:48)
    * @param tempId epoch timestamp ex: 1612350828 (2021-02-03T11:13:48.204018)
    * @param temperate double value
    * @param humidity integer value
    * @return average temperature and average humidity
    */
  private def calcInternalTempAvgs(collection: MongoCollection[Document], timeKey: Long, tempId: String, temperate: Double, humidity: Int): (Double, Int) = {

    // get just necessary information
    val findByTimeKey: SingleObservable[Document] = collection.find(Document("timekey" -> timeKey)).projection(
      Projections.fields(
        Projections.include("timekey", "internal.history.id", "internal.history.temperature", "internal.history.humidity"), Projections.excludeId()
      )
    )

    // get document
    val documentByTimeKey: Option[Document] = findByTimeKey.executeSync()

    documentByTimeKey match {
      case Some(doc: Document) => {
        doc.toBsonValue("internal") match {
          case Some(internal: BsonValue) => {
            val allTempHistory: List[BsonValue] = internal.toListBsonValues("history")

            val filteredTempId = allTempHistory.filter(i => i.toString("id") == tempId)

            // get tempId history entry count + new record
            val sizeFilteredTempIdPlusOne = filteredTempId.size + 1
            // sum stored temperature and humidity
            val sumTemperature = filteredTempId.map(f => f.toDouble("temperature")).sum(Numeric[Double]) + temperate
            val sumHumidity = filteredTempId.map(f => f.toInt("humidity")).sum(Numeric[Int]) + humidity
            // calc averages
            val avgTemperature = sumTemperature / sizeFilteredTempIdPlusOne
            val avgHumidity = sumHumidity / sizeFilteredTempIdPlusOne

            return (avgTemperature, avgHumidity)
          }
          case None => Logger.info("no internal records found")
        }
      }
      case None => Logger.warn("no document found")
    }

    // no record available
    return (temperate, humidity)
  }

  /**
    * Upsert external temperature measurement to existent or new document
    *
    * @param collection mongodb collection
    * @param timeKey epoch timestamp ex: 1612350000 (2021-02-03T11:13:48)
    * @param timestamp epoch timestamp ex: 1612350828 (2021-02-03T11:13:48.204018)
    * @param temperature double value
    * @param asyncUpsert true for async upsert, false for sync upsert
    */
  def externalTemp(collection: MongoCollection[Document], timeKey: Long, timestamp: Long, temperature: Double, asyncUpsert: Boolean=false): Unit = {
    val dateKey = epochSecondstoDate(timeKey)
    val timeDate = epochSecondstoDate(timestamp)

    Logger.info(s"time key: $timeKey time Date: $dateKey")

    // upsert record by hour in unix timestamp
    val upsert = collection.updateOne(
      equal("timekey", timeKey),
      combine(
        set("datekey", dateKey),
        set("external.current", Document("temperature" -> temperature, "timestamp" -> timestamp, "date" -> timeDate)),

        push("external.history", Document("timestamp" -> timestamp, "date" -> timeDate, "temperature" -> temperature)),
      ),
      new UpdateOptions().upsert(true)
    )

    if (asyncUpsert == true) {
      // async upsert
      upsert.executeAsync(
        onResponse = t => Logger.info(s"external timekey $timeKey upserted: $t"), 
        onFail = t => Logger.error(s"upsert for timekey $timeKey failed with error: ${t.getMessage()}")
      )
    } else {
      // sync upsert
      upsert.executeSync() match {
        case Some(v) => Logger.info(s"external timekey $timeKey upserted: $v")
        case None => Logger.error(s"external timekey $timeKey not upserted")
      }
    }
  }

  /**
    * Upsert internal temperature measurement to existent or new document
    *
    * @param collection mongodb collection
    * @param timeKey epoch timestamp ex: 1612350000 (2021-02-03T11:13:48)
    * @param tempId equipment id
    * @param timestamp epoch timestamp ex: 1612350828 (2021-02-03T11:13:48.204018)
    * @param temperature double value
    * @param humidity integer value
    * @param asyncUpsert true for async upsert, false for sync upsert
    */
  def internalTemp(collection: MongoCollection[Document], timeKey: Long, tempId: String, timestamp: Long, temperature: Double, humidity: Int, asyncUpsert: Boolean=false): Unit = {
    // convert epoch seconds to date
    val timeDate = epochSecondstoDate(timestamp)

    // calculate average temperature & humidity
    val (avgTemperature, avgHumidty) = calcInternalTempAvgs(collection, timeKey, tempId, temperature, humidity)

    // upsert device metrics
    val upsert = collection.updateOne(equal("timekey", timeKey), combine(
        set(s"internal.current.$tempId", Document("temperature" -> temperature, "humidity" -> humidity, "date" -> timeDate)),
        set(s"internal.avg.$tempId", Document("temperature" -> avgTemperature, "humidity" -> avgHumidty)),

        push("internal.history", Document("id" -> tempId, "timestamp" -> timestamp, "date" -> timeDate, "temperature" -> temperature, "humidity" -> humidity)),
      ),
      new UpdateOptions().upsert(true)
    )

    if (asyncUpsert == true) {
      // async upsert
      upsert.executeAsync(
        onResponse = t => Logger.info(s"external timekey $timeKey upserted: $t"), 
        onFail = t => Logger.error(s"upsert for timekey $timeKey failed with error: ${t.getMessage()}")
      )
    } else {
      // sync upsert
      upsert.executeSync() match {
        case Some(v) => Logger.info(s"external timekey $timeKey upserted: $v")
        case None => Logger.error(s"external timekey $timeKey not upserted")
      }
    }
  }
}