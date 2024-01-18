package service.db_service

import io.lettuce.core.RedisClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import java.io.FileInputStream
import java.time.Duration
import java.util.Properties
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.control.Breaks.break

object DBService extends App {
  val logger = LoggerFactory.getLogger("my-app")

  val consumerProperties = new Properties()
  consumerProperties.load(new FileInputStream("src/main/resources/consumer.properties"))

  val redisClient = RedisClient.create("redis://localhost:6379")
  val connection = redisClient.connect()
  val topic = "weather-data"


  val list: ListBuffer[String] = getDataFromTopic()
  saveDataToDB(list)

  private def saveDataToDB(dataList: ListBuffer[String]): Unit = {
    dataList.foreach(dataItem => connection.sync().set(dataItem.hashCode.toString, dataItem))

    // Close the Redis connection if it was opened in this method
    if (connection.isOpen) {
      redisClient.shutdown()
    }
  }

  def getDataFromTopic(): ListBuffer[String] = {
    val consumer = new KafkaConsumer[String, String](consumerProperties)
    consumer.subscribe(List(topic).asJava)

    var consumed = true
    val extractedData = ListBuffer[String]()

    while (consumed) {
      // Poll for new messages
      val records = consumer.poll(Duration.ofSeconds(1))
      if (records.isEmpty) break

      val recordIterator = records.iterator()
      while (recordIterator.hasNext) {
        extractedData += recordIterator.next().value()
      }
      consumed = false
    }
    extractedData
  }
}
