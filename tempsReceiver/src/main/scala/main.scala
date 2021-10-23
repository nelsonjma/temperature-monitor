/*
  curl \
    -X POST \
    -H "Content-Type: application/json" \
    -d "{\"temperature\": 32, \"timestamp\": 1612350892}" \
    http://localhost:8081/api/temp/external

  curl \
    -X POST \
    -H "Content-Type: application/json" \
    -d "{\"id\": \"temp0\", \"temperature\": 11.1, \"timestamp\": 1612350898, \"humidity\": 1}" \
    http://localhost:8081/api/temp/internal

  export HTTP_PORT=8081
  export MONGODB_URL=mongodb://localhost:27017
  export MONGODB_DATABASE=metrics
  export MONGODB_COLLECTION=temperature

  java -jar temp-receiver-assembly-1.0.jar
 */

 import lib.Logger
 import worker.Receiver


 object Main {

  def main(args: Array[String]): Unit = {
    // env inputs
    val httpPort = sys.env.getOrElse("HTTP_PORT", "8081")

    Logger.info(s"HTTP_PORT: ${sys.env.getOrElse("HTTP_PORT", "")}")
    Logger.info(s"MONGODB_URL: ${sys.env.getOrElse("MONGODB_URL", "")}")
    Logger.info(s"MONGODB_DATABASE: ${sys.env.getOrElse("MONGODB_DATABASE", "")}")
    Logger.info(s"MONGODB_COLLECTION: ${sys.env.getOrElse("MONGODB_COLLECTION", "")}")
    
    Logger.info(s"\nStart temp sensors receiver")
    
    Receiver.start(httpPort)
  }
 }
