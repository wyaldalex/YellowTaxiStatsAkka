ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.9"

ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.20",
  "com.typesafe.akka" %% "akka-http" % "10.2.10",
  "com.typesafe.akka" %% "akka-stream" % "2.6.20",
  "com.typesafe.akka" %% "akka-coordination" % "2.6.20",
  "com.typesafe.akka" %% "akka-remote" % "2.6.20",
  "com.typesafe.akka" %% "akka-cluster" % "2.6.20",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.6.20",
  "com.typesafe.akka" %% "akka-pki" % "2.6.20",
  "com.typesafe.akka" %% "akka-persistence" % "2.6.20",
  "com.typesafe.akka" %% "akka-persistence-query" % "2.6.20",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.6",
  "io.circe" %% "circe-core" % "0.14.2",
  "io.circe" %% "circe-generic" % "0.14.2",
  "io.circe" %% "circe-parser" % "0.14.2",
  "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
  "ch.qos.logback" % "logback-classic" % "1.3.0",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.2.10" % Test,
  "com.typesafe.akka" %% "akka-testkit" % "2.6.20" % Test,
  "org.scalatest" %% "scalatest" % "3.2.12" % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "YelloTaxiStatsAkka"
  )
