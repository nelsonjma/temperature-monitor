package lib

import java.time.LocalDateTime

object Logger {

  def debug(msg: String): Unit = {
    println(message(msg, "DEBUG"))
  }

  def info(msg: String): Unit = {
    println(message(msg, "INFO"))
  }

  def warn(msg: String): Unit = {
    println(message(msg, "WARNING"))
  }

  def error(msg: String): Unit = {
    println(message(msg, "ERROR"))
  }

  private def message(msg: String, lvlText: String): String = {
    val dtNow = LocalDateTime.now().toString()
    return s"${dtNow} ${lvlText} ${msg}"
  }
}