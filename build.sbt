ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "akka-weather-data"
  )
libraryDependencies ++= Seq(
  "io.lettuce" % "lettuce-core" % "6.3.0.RELEASE",
  "com.typesafe.akka" %% "akka-http" % "10.5.0",
  "com.typesafe.akka" % "akka-stream-kafka_3" % "4.0.2",
  "org.apache.kafka" % "kafka-clients" % "3.4.0",
  "org.slf4j" % "slf4j-api" % "2.0.5",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.typesafe" % "config" % "1.4.2",
  "io.circe" %% "circe-core" % "0.14.5",
  "io.circe" %% "circe-parser" % "0.14.5",
)
