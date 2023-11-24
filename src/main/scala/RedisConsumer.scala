import io.lettuce.core.RedisClient
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import java.io.FileInputStream
import java.time.Duration
import java.util.Properties
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.util.control.Breaks.break

object RedisConsumer extends App {
  val logger = LoggerFactory.getLogger("my-app")

  val consumerProperties = new Properties()
  consumerProperties.load(new FileInputStream("src/main/resources/consumer.properties"))

  val redisClient = RedisClient.create("redis://localhost:6379")
  val connection = redisClient.connect()
  val topic = "weather-data"


  //  var list: ListBuffer[String] = extractDataFromKafkaToList()
  //  saveDataToRedis(list)
  println(getDataFromRedis())

  def getDataFromRedis(): ListBuffer[String] = {
    // Get all the keys in Redis
    val keys = connection.sync().keys("*")

    // Create an empty list buffer to store the retrieved data
    val dataListBuffer = ListBuffer[String]()

    // Iterate over the keys and get the values from Redis
    for (key <- keys.asScala) {
      val value: String = connection.sync().get(key)
      dataListBuffer += value
    }

    dataListBuffer
  }

  def saveDataToRedis(dataList: ListBuffer[String]): Unit = {
    for (dataItem <- dataList) {
      connection.sync().set(dataItem.hashCode.toString, dataItem)
    }

    // Close the Redis connection if it was opened in this method
    if (connection.isOpen) {
      redisClient.shutdown()
    }
  }

  def extractDataFromKafkaToList(): ListBuffer[String] = {
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
