package com.tudux.taxi.actors.loader

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.CreateTaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.CreateTaxiTripExtraInfo
import com.tudux.taxi.actors.implicits.TaxiTripImplicits._
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.CreateTaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.CreateTaxiTripTimeInfo
import com.tudux.taxi.app.ShardedActorsGenerator.{createShardedCostActor, createShardedExtraInfoActor, createShardedPassengerInfoActor, createShardedTimeInfoActor}
import com.typesafe.config.ConfigFactory

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success}

case class TaxiTripEntry(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                         tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                         storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                         paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                         tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)

sealed trait TaxiTripCommand
object TaxiTripCommand {
  case class CreateTaxiTripCommand(taxiTrip: TaxiTripEntry, tripId: String = "") extends  TaxiTripCommand
}

object TaxiStatAppLoader extends App {
  //testing sharding of cost actor
  val config = ConfigFactory.parseString(
    "akka.remote.artery.canonical.port = 2551".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher

  val costAggregatorActor : ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor : ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

  val parentCostShardRegionRef: ActorRef = createShardedCostActor(system,costAggregatorActor)
  val parentExtraInfoShardRegionRef: ActorRef = createShardedExtraInfoActor(system)
  val parentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val parentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system,timeAggregatorActor)

  import kantan.csv._
  import kantan.csv.ops._ // Automatic derivation of codecs.
  implicit val decoder: RowDecoder[TaxiTripEntry] = RowDecoder.ordered(TaxiTripEntry.apply _)
  //Start Processing including reading of the file
  val startTimeMillis = System.currentTimeMillis()
  //val source_csv = Source.fromResource("smallset.csv").mkString
  //val source_csv = Source.fromResource("100ksample.csv").mkString
  val source_csv = Source.fromResource("1ksample.csv").mkString
  //val source_csv = Source.fromResource("10ksample.csv").mkString
  //val source_csv = Source.fromResource("1millSample.csv").mkString
  val reader = source_csv.asCsvReader[TaxiTripEntry](rfc)
  //TODO: More robust loader, buisiness need

  object ActorCounter {
    def props: Props = Props(new ActorCounter)
    case class IncreaseCount(operationResponse: (OperationResponse,OperationResponse,OperationResponse,OperationResponse))
  }
  class ActorCounter extends Actor with ActorLogging {
    import ActorCounter._
    var counter = 1
    override def receive: Receive = {
      case IncreaseCount(operationResponse) =>
        log.info(s"TaxiTrip Info Loaded Number: $counter with values ${operationResponse.toString}")
        counter += 1
    }
  }
  val actorCounter = system.actorOf(ActorCounter.props)

  import ActorCounter._
  import akka.pattern.ask

  reader.foreach(either => {
    val tripId = UUID.randomUUID().toString
    println(s"Persisting Id $tripId")
    //Scapegoat related refactoring
    val defaultTaxiEntryVal = TaxiTripEntry(0, "", "", 0, 0, 0, 0, 0, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    val defaultVal: TaxiTripEntry = either.right.getOrElse(defaultTaxiEntryVal)

    val costResponse = (parentCostShardRegionRef ? CreateTaxiTripCost(tripId,defaultVal)).mapTo[OperationResponse]
    val extraInfoResponse = (parentExtraInfoShardRegionRef ? CreateTaxiTripExtraInfo(tripId, defaultVal)).mapTo[OperationResponse]
    val passengerResponse = (parentPassengerShardRegionRef ? CreateTaxiTripPassengerInfo(tripId, defaultVal)).mapTo[OperationResponse]
    val timeResponse = (parentTimeInfoShardRegionRef ? CreateTaxiTripTimeInfo(tripId, defaultVal)).mapTo[OperationResponse]
    val combineResponse = for {
      r1 <- costResponse
      r2 <- extraInfoResponse
      r3 <- passengerResponse
      r4 <- timeResponse
    } yield (r1, r2, r3, r4)
    combineResponse.onComplete{
      case Success(value) =>
        actorCounter ! IncreaseCount(value)
      case Failure(exception) =>
        println(exception.getMessage)
    }
  })
}
