import akka.http.scaladsl.Http
import controller.WeatherController
import kafka_client.ImplicitVals
import org.slf4j.LoggerFactory

object Main extends ImplicitVals{
  val logger = LoggerFactory.getLogger("my-app")

  def main(args: Array[String]): Unit = {
    val weatherRoute = WeatherController.weatherRoute

    Http().newServerAt("localhost", 8080).bind(weatherRoute)
    logger.info("Server online at http://localhost:8080/weather")
  }
}
