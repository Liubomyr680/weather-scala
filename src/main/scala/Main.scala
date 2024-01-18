import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import controller.WeatherController
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

object Main {
  val logger = LoggerFactory.getLogger("my-app")

  implicit val system: ActorSystem = ActorSystem("weather-app")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def main(args: Array[String]): Unit = {
    val weatherRoute = WeatherController.weatherRoute

    Http().newServerAt("localhost", 8080).bind(weatherRoute)
    logger.info("Server online at http://localhost:8080/weather")
  }
}
