package com.tudux.taxi.app

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, Offset, PersistenceQuery}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout
import com.tudux.taxi.actors.cost.TaxiTripCostAdapter
import com.tudux.taxi.actors.cost.TaxiTripCostDataModel.WrittenTaxiTripCostCreated
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object QueryApp extends App {

  val log: Logger = LoggerFactory.getLogger(QueryApp.getClass.getName)

  implicit val timeout: Timeout = Timeout(2.seconds)

  // testing sharding of cost actor
  val config = ConfigFactory.parseString(
    "akka.remote.artery.canonical.port = 2551".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  implicit val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  implicit val scheduler: ExecutionContext = system.dispatcher
  //read journal
  val readJournal = PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val persistenceIds = readJournal.persistenceIds()
  //val rockPlaylists = readJournal.eventsByTag("rock", Offset.noOffset)

  val completedOrders: Source[EventEnvelope, NotUsed] =
    readJournal.eventsByTag("2", Offset.noOffset)

  val firstCompleted: Future[Vector[WrittenTaxiTripCostCreated]] =
    completedOrders
      .map(_.event)
      .collectType[WrittenTaxiTripCostCreated]
      .take(2) // cancels the query stream after pulling 10 elements
      .runFold(Vector.empty[WrittenTaxiTripCostCreated])(_ :+ _)

  val logSink = Sink.foreach[String](x => log.info(s"Value result ${x}"))
  firstCompleted.onComplete{
    case Success(value) => log.info(s"Completed query $value")
    case Failure(exception) =>  log.info(s"Failed query with $exception")
  }

  //persistenceIds.runWith(logSink)
//  rockPlaylists.runForeach { event =>
//    log.info(s"Found by tag: $event")
//  }



}
