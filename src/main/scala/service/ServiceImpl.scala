package service

import com.example.{City, GetCityWeather, WeatherData}
import io.lettuce.core.RedisClient
import io.lettuce.core.api.sync.RedisCommands

import scala.concurrent.Future

class ServiceImpl extends GetCityWeather {

  val redisClient = RedisClient.create("redis://localhost:6379")
  val connection = redisClient.connect()
  val syncCommands: RedisCommands[String, String] = connection.sync()

  override def getData(cityName: City): Future[WeatherData] = {

    val redisKey = cityName.name
    connection.sync().get(redisKey)
    // Check if the city key exists in Redis
    val cityExists = syncCommands.exists(redisKey) == 1

    // If the city exists, fetch the values from Redis
    if (cityExists) {
      val temp = syncCommands.get(s"$redisKey:temp").toDouble
      val humidity = syncCommands.get(s"$redisKey:humidity").toDouble
      val isRainy = syncCommands.get(s"$redisKey:isRainy").toBoolean

      Future.successful(WeatherData(cityName.name, temp, humidity, isRainy))
    } else {
      Future.failed(new RuntimeException(s"City not found: ${cityName.name}"))
    }
  }
}
