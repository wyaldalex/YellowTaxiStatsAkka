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
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
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
/*
sealed trait TaxiTripResponse
object TaxiTripResponse {
  case class TaxiTripCreatedResponse(tripId: String) extends TaxiTripResponse

}

object TaxiTripActor {
  def props(parentCostShardedActor: ActorRef,parentExtraInfoShardedActor: ActorRef,
            parentPassengerShardedActor: ActorRef,parentTimeShardedActor: ActorRef): Props =
    Props(new TaxiTripActor(parentCostShardedActor,parentExtraInfoShardedActor,
      parentPassengerShardedActor, parentTimeShardedActor))

}
class TaxiTripActor(parentCostShardedActor: ActorRef,parentExtraInfoShardedActor: ActorRef,
                    parentPassengerShardedActor: ActorRef,parentTimeShardedActor: ActorRef) extends Actor with ActorLogging {

  import TaxiTripCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
  import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand._
  import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand._
  import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand._

  override def receive : Receive = {
    case CreateTaxiTripCommand(taxiTrip,_) =>
      //generate new stat ID to avoid conflicts
      val tripId = UUID.randomUUID().toString
      log.info(s"Received $taxiTrip to create")
      import akka.pattern.{ask, pipe}
      implicit val ec: ExecutionContext = context.dispatcher
      implicit val timeout: Timeout = 5.seconds

      val costFuture = (parentCostShardedActor ? CreateTaxiTripCost(tripId,taxiTrip))
      val extraInfoFuture = (parentExtraInfoShardedActor ? CreateTaxiTripExtraInfo(tripId,taxiTrip))
      val passengerFuture = (parentPassengerShardedActor ? CreateTaxiTripPassengerInfo(tripId,taxiTrip))
      val timeFuture = (parentTimeShardedActor ? CreateTaxiTripTimeInfo(tripId,taxiTrip))

      //sender() ! TaxiTripCreatedResponse(tripId)
      //Future(costFuture,extraInfoFuture,passengerFuture,timeFuture).pipeTo(sender())
      sender ! (costFuture,extraInfoFuture,passengerFuture,timeFuture)
    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
//      taxiTripCostActor.forward(printTimeToLoad)
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

} */

object TaxiStatAppLoader extends App {

  //testing sharding of cost actor
  val config = ConfigFactory.parseString(
    s"""
       |akka.remote.artery.canonical.port = 2551
""".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  implicit val timeout: Timeout = Timeout(10.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher
  /*
   val localStoreActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
   *///
  //val persistentTaxiStatActor = system.actorOf(PersistentTaxiStatActor.props, "quickPersistentActorTest")
  //Somehow create the cost actor sharded version
  val costAggregatorActor : ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor : ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

  val parentCostShardRegionRef: ActorRef = createShardedCostActor(system,costAggregatorActor)
  val parentExtraInfoShardRegionRef: ActorRef = createShardedExtraInfoActor(system)
  val parentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val parentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system,timeAggregatorActor)

  /*val taxiTripActor = system.actorOf(TaxiTripActor.props(parentCostShardRegionRef,parentExtraInfoShardRegionRef,
    parentPassengerShardRegionRef,parentTimeInfoShardRegionRef), "parentTaxiActor") */

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

  //Give time for cluster to start up
  Thread.sleep(60000)
  import TaxiTripCommand._
  //Data loading:
//  reader.foreach(either => {
//    taxiTripActor ! CreateTaxiTripCommand((either.right.getOrElse(TaxiTripEntry(
//      2,"2015-01-15 19:05:39","2015-01-15 19:23:42",1,1.59,-73.993896484375,40.750110626220703,1,"N",-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05
//    ))))
//  })
  //TODO: More robust loader, buisiness need
  var loaderCounter = 1
  import akka.pattern.ask
  reader.foreach(either => {
    val tripId = UUID.randomUUID().toString
    println(s"Persisting Id $tripId")
    val costResponse = (parentCostShardRegionRef ? CreateTaxiTripCost(tripId, either.right.get)).mapTo[OperationResponse]
    val extraInfoResponse = (parentExtraInfoShardRegionRef ? CreateTaxiTripExtraInfo(tripId, either.right.get)).mapTo[OperationResponse]
    val passengerResponse = (parentPassengerShardRegionRef ? CreateTaxiTripPassengerInfo(tripId, either.right.get)).mapTo[OperationResponse]
    val timeResponse = (parentTimeInfoShardRegionRef ? CreateTaxiTripTimeInfo(tripId, either.right.get)).mapTo[OperationResponse]
    val combineResponse = for {
      r1 <- costResponse
      r2 <- extraInfoResponse
      r3 <- passengerResponse
      r4 <- timeResponse
    } yield (r1, r2, r3, r4)
    println(s"Combined response $combineResponse")
    combineResponse.onComplete{
      case Success(value) =>
        println(s"Loader response $value for entry $loaderCounter and id $tripId" )
        loaderCounter += 1
      case Failure(exception) =>
        println(exception.getMessage)
    }
  })

}
