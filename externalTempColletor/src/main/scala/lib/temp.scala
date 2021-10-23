package lib

import org.jsoup.Jsoup
import scalaj.http.Http
import scalaj.http.HttpResponse

object Temp {

  // FROM: <b><font color="#3366FF">10.5°C&nbsp;</font></b> / TO: 10.5
  def filterTemp(value: String): String = {

    if (value.contains("°")) {
      try {
        // 0 => b><font color="#3366FF">10.5
        val subMsg = value.split("°")(0)
        // <b><font color="#3366FF"> index of last ">"  
        val idxLastDelimitator = subMsg.lastIndexOf(">")
        // fetch 10.5
        val strTemp = subMsg.substring(idxLastDelimitator + 1)
        return strTemp
      } catch {
        case e: java.lang.NumberFormatException => {
          // has not valid temp
          return "-99"
        }
        case e: Exception => {
          // error parsing temperature from string
          return "-100"
        }
      }
    }

    // does not have temp value in input data
    return "-98"
  }

  def getPage(url: String): String = {
    try {
      val doc = Jsoup.connect(url).get()
      val temp = doc.select("b")

      // no data returned
      if (temp.isEmpty()) { return "" }
      
      val tempValue = temp.get(1).clone().toString()  // because paranoia
      return tempValue
    } catch {
      case e: Exception => { return "" }
    }

    return ""
  }

  def sendToStorage(url: String, temp: String, fetch_at: String): Unit = {
    Logger.info(s"temperature: $temp, fetch at $fetch_at")

    try {
      // val response: HttpResponse[String] = Http(url).param("temp", temp).param("fetch_at", fetch_at).asString // GET
      val data: String = f"""{"temperature": $temp, "timestamp": $fetch_at}"""
      val response: HttpResponse[String] = Http(url).postData(data).header("content-type", "application/json").asString // POST
      Logger.info(s"reponse code: ${response.code.toString()}")
      Logger.info(s"reponse body: \n${response.body.toString()} \n")
    } catch {
      case e: Exception => {
        Logger.error(s"failed to send temp data to ${url} with error: ${e.toString()}")
      }
    }
  }
}