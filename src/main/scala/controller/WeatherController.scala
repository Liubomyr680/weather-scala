package controller

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory
import io.circe.parser.parse
import kafka_client.{WeatherConsumer, WeatherProducer}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object WeatherController {

  private val logger = LoggerFactory.getLogger("my-app")
  implicit val system: ActorSystem = ActorSystem("weather-app")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Fetch weather data from the public API
  val apiKey = "b6dc699e13e642b7bf9134734232011"
  val weatherDataUrl = s"http://api.weatherapi.com/v1/current.json?key=$apiKey&q=London"
  val weatherRequest = HttpRequest(uri = weatherDataUrl)

  val weatherRoute: Route = {
    get {
      path("weather") {
        complete {
          Future {

            val feature = Http().singleRequest(weatherRequest)
            feature.onComplete {
              case Success(response) =>
                response.entity.toStrict(10.second).onComplete {
                  case Success(byteString) =>
                    val json = byteString.data.utf8String
                    val weatherData = parse(json)

                    if(WeatherConsumer.consumeIsEmpty(weatherData)){
                      WeatherProducer.sendDataToTopic(weatherData)
                    }

                  case Failure(error) =>
                    logger.info(s"Error: ${error.getMessage}")
                }
              case Failure(error) =>
                logger.info(s"Error: ${error.getMessage}")
            }

            // Return a placeholder response while the asynchronous weather data retrieval is in progress
            HttpEntity(ContentTypes.`application/json`, "data retrieving...")
          }
        }
      }
    }
  }
}
