
import Weather.{executionContext, materializer}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.kafka.scaladsl._
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.{Config, ConfigFactory}
import io.circe._
import io.circe.parser.parse
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import org.slf4j.LoggerFactory

import scala.jdk.FutureConverters.FutureOps
import io.circe.parser.decode

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Weather extends App {
  private val logger = LoggerFactory.getLogger("my-app")
  implicit val system: ActorSystem = ActorSystem("weather-app")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Fetch weather data from the public API
  val topic = "weather-data"
  val apiKey = "b6dc699e13e642b7bf9134734232011"
  val weatherDataUrl = s"http://api.weatherapi.com/v1/current.json?key=$apiKey&q=London"
  val weatherRequest = HttpRequest(uri = weatherDataUrl)

  val config = ConfigFactory.load()

  val weatherConsumer = new WeatherConsumer(system, topic)
  val weatherDataSender = new WeatherDataSender(config, topic)

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

                    if(weatherConsumer.consumeIsEmpty(weatherData)){
                      weatherDataSender.sendDataToTopic(weatherData)
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
  Http().newServerAt("localhost", 8080).bind(weatherRoute)

  logger.info("Server online at http://localhost:8080/weather")
}

class WeatherConsumer(system: ActorSystem, topic: String) {
  private val logger = LoggerFactory.getLogger("my-app")
  val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer).withGroupId("group-id-1")
  def consumeIsEmpty(weatherData: Either[Throwable, Json]): Boolean = {

    // Create a Consumer
    val consumer = Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
    // Check for record existence
    val hasRecord: Boolean = consumer
      .map { record =>
        val jsonString = record.value()
        decode[Json](jsonString)
      }
      .runWith(Sink.headOption) // Get the first element (if any)
      .map(_.nonEmpty)// Check if an element exists
      .asJava
      .toCompletableFuture // Convert to a Future
      .get() // result

    hasRecord
  }
}

class WeatherDataSender(config: Config, topic: String) {
  private val logger = LoggerFactory.getLogger("my-app")
  def sendDataToTopic(weatherData: Either[Throwable, Json]): Future[Unit] = {
    val producerSettings = ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)
    val producer = Producer.plainSink(producerSettings)

    Source.single(weatherData.toString)
      .map(value => new ProducerRecord[String, String](topic, value))
      .runWith(producer)
      .map(_ => logger.info(s"The data was successfully sent to $topic"))
      .recover {
        case error => logger.error(s"Failed to send data to $topic. Error: $error")
      }
  }
}
