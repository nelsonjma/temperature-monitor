import lib.Logger
import lib.Temp

import java.time.LocalDateTime;
import java.time.ZoneOffset;

object Main {

  def main(args: Array[String]): Unit = {
    val logger = Logger

    val tempUrl = sys.env.getOrElse("TEMP_URL", "http://localhost/dados-detalhados")
    val sleepInterval = sys.env.getOrElse("SLEEP", "60").toInt
    val storageUrl = sys.env.getOrElse("STORAGE_URL", "http://localhost:1681/api/temp/external")

    while(true) {
      collect(tempUrl, sleepInterval, storageUrl)
    }
  }

  def collect(tempUrl: String, sleepInterval: Int, storageUrl: String): Unit = {
      val temp = Temp
      Logger.info(tempUrl)

      // fetch page temperature line
      val tempLine = temp.getPage(tempUrl)
      // process line and return temperature value
      val tempValue = temp.filterTemp(tempLine)
      // fetch temp date
      val fetchAt = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)

      tempValue match {
        case "-98" => {
          Logger.error(s"$tempValue: does not have temp value in input data")
          temp.sendToStorage(storageUrl, "0", fetchAt.toString())
        }
        case "-99" => {
          Logger.error(s"$tempValue: has not valid temp")
          temp.sendToStorage(storageUrl, "0", fetchAt.toString())
        }
        case "-100" => {
          Logger.error(s"$tempValue: error parsing temperature from string")
          temp.sendToStorage(storageUrl, "0", fetchAt.toString())
        }
        case _ => {
          temp.sendToStorage(storageUrl, tempValue, fetchAt.toString())
        }
      }

      // seconds * 1000
      Thread.sleep(sleepInterval * 1000)
  }
}