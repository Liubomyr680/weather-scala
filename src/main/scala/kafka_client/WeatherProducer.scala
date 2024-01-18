package kafka_client

import akka.kafka.ProducerSettings
import akka.kafka.scaladsl._
import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import io.circe._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object WeatherProducer extends ImplicitVals {
  private val logger = LoggerFactory.getLogger("my-app")
  val config: Config = ConfigFactory.load()
  val topic = "weather-data"

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
