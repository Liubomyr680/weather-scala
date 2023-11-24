
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.*
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.Materializer
import io.circe.*
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.slf4j.LoggerFactory

import java.io.FileInputStream
import java.time.Duration
import java.util.Properties
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success}

object Weather extends App {
  val logger = LoggerFactory.getLogger("my-app")
  implicit val system: ActorSystem = ActorSystem("weather-app")
  implicit val materializer: Materializer = Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  // Fetch weather data from the public API
  val topic = "weather-data"
  val apiKey = "b6dc699e13e642b7bf9134734232011"
  val weatherDataUrl = s"http://api.weatherapi.com/v1/current.json?key=$apiKey&q=London"
  val weatherRequest = HttpRequest(uri = weatherDataUrl)

  val producerProperties = new Properties()
  val consumerProperties = new Properties()
  consumerProperties.load(new FileInputStream("src/main/resources/consumer.properties"))
  producerProperties.load(new FileInputStream("src/main/resources/producer.properties"))


  val weatherDataSender = new WeatherDataSender(producerProperties, topic)
  val weatherConsumer = new WeatherConsumer(consumerProperties, topic)

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

                    if (!weatherConsumer.consumeIsEmpty(weatherData)) {
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
  val bindingFuture = Http().newServerAt("localhost", 8080).bindFlow(weatherRoute)
  logger.info("Server online at http://localhost:8080/weather")
}

class WeatherConsumer(consumerProperties: Properties, topic: String) {
  val logger = LoggerFactory.getLogger("my-app")

  def consumeIsEmpty(weatherData: Either[Throwable, Json]): Boolean = {

    val consumer = new KafkaConsumer[String, String](consumerProperties)
    consumer.subscribe(List(topic).asJava)
    var consumed = false

    while (!consumed) {
      val records = consumer.poll(Duration.ofSeconds(3)) // Poll for new messages

      if (records.isEmpty) {
        consumed = true
        logger.info(s"$topic is empty...")
      }

      val recordIterator = records.iterator()
      while (recordIterator.hasNext) {
        val value = recordIterator.next()
        if (value.value() == weatherData.toString) {
          consumer.close() // Message found, stop consuming
          consumed = true
        }
      }
    }
    consumer.close()
    consumed
  }
}

class WeatherDataSender(producerProperties: Properties, topic: String) {
  val logger = LoggerFactory.getLogger("my-app")

  def sendDataToTopic(weatherData: Either[Throwable, Json]): Unit = {
    val producer = new KafkaProducer[String, String](producerProperties)
    producer.send(new ProducerRecord[String, String](topic, weatherData.toString))
    producer.flush()
    logger.info(s"The data was successfully sent to the $topic ")
  }
}
