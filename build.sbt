name := "YellowTaxiStatsAkka"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.12.12"

scapegoatVersion in ThisBuild := "1.3.9"
scapegoatReports := Seq("xml")
scalacOptions in Scapegoat += "-P:scapegoat:overrideLevels:all=Warning"

lazy val akkaHttpVersion = "10.2.8"
//lazy val akkaHttpVersion = "10.1.8"
//lazy val akkaVersion = "2.5.21"
//lazy val protobufVersion = "3.6.1"
lazy val akkaVersion     = "2.6.9"
lazy val circeVersion    = "0.14.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"                  % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed"     % akkaVersion,
  "com.datastax.oss"  %  "java-driver-core"           % "4.13.0",
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5",
  "io.circe"          %% "circe-core"                 % circeVersion,
  "io.circe"          %% "circe-generic"              % circeVersion,
  "io.circe"          %% "circe-parser"               % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe"            % "1.39.2",
  "ch.qos.logback"    % "logback-classic"             % "1.2.10",
  "com.github.marklister" %% "product-collections" % "1.4.5",
  "com.chuusai" %% "shapeless" % "2.3.3",
  "com.nrinaudo" %% "kantan.csv-generic" % "0.7.0",
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  //"com.github.swagger-akka-http" %% "swagger-akka-http" % "1.4.0",
  "com.github.swagger-akka-http" %% "swagger-akka-http" % "1.3.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.11.3",
  "javax.xml.bind" % "jaxb-api" % "2.1",
  //"com.github.swagger-akka-http" %% "swagger-akka-http" % "0.14.1",
  //"com.github.swagger-akka-http" % "swagger-akka-http_2.11" % "0.14.0",
  //"com.typesafe.akka" % "akka-http-testkit_2.11" % "10.0.9" % "test",
 // "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test,
  //clustering related
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  //"io.aeron" % "aeron-driver" % "1.38.1",
  //"io.aeron" % "aeron-client" % "1.38.1",
  "io.aeron" % "aeron-driver" % "1.15.0",
  "io.aeron" % "aeron-client" % "1.15.0",

  //akka streams
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion,

  // optional, if you want to add tests
  "com.typesafe.akka" %% "akka-http-testkit"          % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed"   % akkaVersion     % Test,
  "org.scalatest"     %% "scalatest"                  % "3.2.14"         % Test
)
