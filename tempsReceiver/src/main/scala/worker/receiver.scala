package worker

import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import spray.json._

import lib.Logger
import worker.{ ExternalTempInput, InternalTempInput, InputsJsonProtocol }
import worker.{Collector, Message}


object Receiver extends InputsJsonProtocol with SprayJsonSupport {

  implicit val system = akka.actor.typed.ActorSystem(Behaviors.empty, "Receiver")

  // create & start a collector actor
  val collectorSystem = akka.actor.ActorSystem("Collector")
  val worker = collectorSystem.actorOf(akka.actor.Props[Collector], name = "collector-worker")

  // POST: localhost:8081/api/temp/external
  val externalRoute: Route = (path("api" / "temp" / "external") & post) {
    entity(as[ExternalTempInput]) { temp: ExternalTempInput => 
      Logger.debug(s"received external data ${temp.toString()}")
      worker ! Message(temp)
      complete("ok")
    }
  }

  // POST: localhost:8081/api/temp/internal
  val internalRoute: Route = (path("api" / "temp" / "internal") & post) {
    entity(as[InternalTempInput]) { temp: InternalTempInput => 
      Logger.debug(s"received internal data record: ${temp.toString()}")
      worker ! Message(temp)
      complete("ok")
    }
  }

  def start(httpPort: String): Unit = {   
    // join routes
    val routes: Route = concat(externalRoute, internalRoute)

    // start http receiver
    Http().newServerAt("0.0.0.0", httpPort.toInt).bind(routes)
  }
}