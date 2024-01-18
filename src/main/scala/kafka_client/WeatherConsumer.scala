package kafka_client


import akka.kafka.scaladsl._
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import io.circe._
import io.circe.parser.decode
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory

import scala.jdk.FutureConverters.FutureOps

object WeatherConsumer extends ImplicitVals {
  private val logger = LoggerFactory.getLogger("my-app")
  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer).withGroupId("group-id-1")
  val topic = "weather-data"

  def consumeIsEmpty(weatherData: Either[Throwable, Json]): Boolean = {

    // Create a Consumer
    val consumer = Consumer.plainSource(consumerSettings, Subscriptions.topics(topic))
    logger.info("Consumer has been created")
    // Check for record existence
    val hasRecord: Boolean = consumer
      .map { record =>
        val jsonString = record.value()
        decode[Json](jsonString)
      }
      .runWith(Sink.headOption) // Get the first element (if any)
      .map(_.nonEmpty) // Check if an element exists
      .asJava
      .toCompletableFuture // Convert to a Future
      .get() // result

    hasRecord
  }
}
