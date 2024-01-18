package kafka_client


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

object WeatherConsumer extends ImplicitVals {
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
      .map(_.nonEmpty) // Check if an element exists
      .asJava
      .toCompletableFuture // Convert to a Future
      .get() // result

    hasRecord
  }
}
