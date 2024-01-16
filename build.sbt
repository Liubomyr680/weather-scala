ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.4"
resolvers += "Akka library repository".at("https://repo.akka.io/maven")
val AkkaVersion = "2.8.0"


lazy val root = (project in file("."))
  .settings(
    name := "akka-weather-data"
  )

//Compile / PB.targets := Seq(
//  scalapb.gen() -> (Compile / sourceManaged).value
//)

//addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0")

libraryDependencies ++= Seq(
  "io.lettuce" % "lettuce-core" % "6.3.0.RELEASE",
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.akka" %% "akka-http" % "10.5.0",
  "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.0",
  "com.typesafe.akka" %% "akka-stream" % "2.8.0",
//  "org.apache.kafka" % "kafka-clients" % "3.4.0",
//  "com.typesafe.akka" %% "akka-kafka" % "2.6.20",
//  "com.typesafe.akka" % "akka-stream-kafka_3" % "4.0.2",
//  "org.apache.kafka" % "kafka-clients" % "3.4.0",
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "io.circe" %% "circe-core" % "0.14.5",
  "io.circe" %% "circe-parser" % "0.14.5",

  "io.grpc" % "grpc-netty" % "1.53.0",
  "io.grpc" % "grpc-protobuf" % "1.53.0",
  "io.grpc" % "grpc-stub" % "1.53.0"
)
