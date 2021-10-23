package lib

// console colors: https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
import java.time.LocalDateTime

object Logger {

  def debug(msg: String): Unit = {
    println(message(msg, "DEBUG", ""))
  }

  def info(msg: String): Unit = {
    println(message(msg, "INFO", "\u001b[32m"))
  }

  def warn(msg: String): Unit = {
    println(message(msg, "WARNING", "\u001b[33m"))
  }

  def error(msg: String): Unit = {
    println(message(msg, "ERROR", "\u001b[31m"))
  }

  private def message(msg: String, lvlText: String, lvlColor: String): String = {
    val dtNow = LocalDateTime.now().toString()
    return s"${lvlColor}${dtNow} ${lvlText} ${msg}\u001b[0m"
  }
}