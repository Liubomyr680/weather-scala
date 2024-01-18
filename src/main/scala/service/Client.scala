package service

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import com.example.{City, GetCityWeatherClient}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Client {

  def main(args: Array[String]): Unit = {
    implicit val sys: ActorSystem = ActorSystem("Client")
    implicit val ec: ExecutionContext = sys.dispatcher

    val client = GetCityWeatherClient(GrpcClientSettings.fromConfig("service.GetCityWeather"))

    val names =
      if (args.isEmpty) List("Alice", "Bob")
      else args.toList

    names.foreach(singleRequestReply)

    def singleRequestReply(name: String): Unit = {
      println(s"Performing request: $name")
      val reply = client.getData(City(name))
      reply.onComplete {
        case Success(msg) =>
          println(msg)
        case Failure(e) =>
          println(s"Error: $e")
      }
    }
  }
}
