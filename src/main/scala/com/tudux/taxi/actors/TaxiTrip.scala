package com.tudux.taxi.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.cluster.sharding.ShardRegion
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.GetTaxiTripCost
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.app.ShardedActorsGenerator.{createShardedCostActor, createShardedExtraInfoActor, createShardedPassengerInfoActor, createShardedTimeInfoActor}
import com.typesafe.config.ConfigFactory

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source


case class TaxiTripEntry(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                         tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                         storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                         paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                         tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)

sealed trait TaxiTripCommand
object TaxiTripCommand {
  case class CreateTaxiTripCommand(taxiTrip: TaxiTripEntry, tripId: String = null) extends  TaxiTripCommand
  case class CreateTaxiTrip(taxiTrip: TaxiTripEntry) extends TaxiTripCommand
  case class DeleteTaxiTrip(tripId: String) extends TaxiTripCommand
}
sealed trait TaxiTripEvent
object TaxiTripEvent {
  case class CreatedTaxiTripEvent(tripId: String) extends  TaxiTripEvent
}

sealed trait TaxiTripResponse
object TaxiTripResponse {
  case class TaxiTripCreatedResponse(tripId: String) extends TaxiTripResponse

}

object TaxiTripActor {
  def props(parentCostShardedActor: ActorRef,parentExtraInfoShardedActor: ActorRef,
            parentPassengerShardedActor: ActorRef,parentTimeShardedActor: ActorRef,
            costAggregatorActor : ActorRef, timeAggregatorActor : ActorRef): Props =
    Props(new TaxiTripActor(parentCostShardedActor,parentExtraInfoShardedActor,
      parentPassengerShardedActor, parentTimeShardedActor, costAggregatorActor, timeAggregatorActor))

}
class TaxiTripActor(parentCostShardedActor: ActorRef,parentExtraInfoShardedActor: ActorRef,
                    parentPassengerShardedActor: ActorRef,parentTimeShardedActor: ActorRef,
  costAggregatorActor : ActorRef, timeAggregatorActor : ActorRef) extends Actor with ActorLogging {

  import TaxiTripCommand._
  import TaxiTripResponse._
  import com.tudux.taxi.actors.aggregators.CostAggregatorCommand._
  import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
  import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand._
  import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand._
  import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand._

  override def receive : Receive = {
    case CreateTaxiTripCommand(taxiTrip,_) =>
      //generate new stat ID to avoid conflicts
      val tripId = UUID.randomUUID().toString
      log.info(s"Received $taxiTrip to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiTrip))

      /*
      new state modification
       */
        //O.O Avoid same Id for persistent actors! Circle and Infinite Loop Warning!!!
      parentCostShardedActor ! CreateTaxiTripCost(tripId,taxiTrip)
      parentExtraInfoShardedActor ! CreateTaxiTripExtraInfo(tripId,taxiTrip)
      parentPassengerShardedActor ! CreateTaxiTripPassengerInfo(tripId,taxiTrip)
      parentTimeShardedActor ! CreateTaxiTripTimeInfo(tripId,taxiTrip)
      costAggregatorActor ! AddCostAggregatorValues(tripId,taxiTrip)
      timeAggregatorActor ! AddTimeAggregatorValues(taxiTrip)

      sender() ! TaxiTripCreatedResponse(tripId)

    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(tripId) =>
      parentCostShardedActor ! DeleteTaxiTrip(tripId)
      parentExtraInfoShardedActor ! DeleteTaxiTrip(tripId)
      parentPassengerShardedActor ! DeleteTaxiTrip(tripId)
      parentTimeShardedActor ! DeleteTaxiTrip(tripId)
    //Domain Specific Operations
    case calculateTripDistanceCost@CalculateTripDistanceCost(_) =>
      log.info("Received CalculateTripDistanceCost request")
      costAggregatorActor.forward(calculateTripDistanceCost)
    case getAverageTripTime@GetAverageTripTime =>
      timeAggregatorActor.forward(getAverageTripTime)
    case getAverageTipAmount@GetAverageTipAmount =>
      log.info("Received GetAverageTipAmount request")
      costAggregatorActor.forward(getAverageTipAmount)

    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
//      taxiTripCostActor.forward(printTimeToLoad)
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

}

object TaxiStatAppLoader extends App {

  //testing sharding of cost actor
  val config = ConfigFactory.parseString(
    s"""
       |akka.remote.artery.canonical.port = 2551
""".stripMargin).withFallback(ConfigFactory.load("sharded/shardedConfigSettings.conf"))

  val system: ActorSystem = ActorSystem("YellowTaxiCluster", config)
  implicit val timeout: Timeout = Timeout(2.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher
  /*
   val localStoreActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
   *///
  //val persistentTaxiStatActor = system.actorOf(PersistentTaxiStatActor.props, "quickPersistentActorTest")
  object CostActorShardingSettings {

    val numberOfShards = 10 // use 10x number of nodes in your cluster
    val numberOfEntities = 100 //10x number of shards
    //this help to map the corresponding message to a respective entity
    val extractEntityId: ShardRegion.ExtractEntityId = {
      case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat,statId) =>
        val entityId = statId.hashCode.abs % numberOfEntities
        (entityId.toString, createTaxiTripCommand)
      case msg@GetTaxiTripCost(statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        (shardId.toString,msg)
    }

    //this help to map the corresponding message to a respective shard
    val extractShardId: ShardRegion.ExtractShardId = {
      case CreateTaxiTripCommand(taxiStat,statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        shardId.toString
      case GetTaxiTripCost(statId) =>
        val shardId = statId.hashCode.abs % numberOfShards
        shardId.toString
      case ShardRegion.StartEntity(entityId) =>
        (entityId.toLong % numberOfShards).toString
    }

  }

  //Somehow create the cost actor sharded version
  val costAggregatorActor : ActorRef = system.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor : ActorRef = system.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

  val parentCostShardRegionRef: ActorRef = createShardedCostActor(system,costAggregatorActor)
  val parentExtraInfoShardRegionRef: ActorRef = createShardedExtraInfoActor(system)
  val parentPassengerShardRegionRef: ActorRef = createShardedPassengerInfoActor(system)
  val parentTimeInfoShardRegionRef: ActorRef = createShardedTimeInfoActor(system,timeAggregatorActor)

  val taxiTripActor = system.actorOf(TaxiTripActor.props(parentCostShardRegionRef,parentExtraInfoShardRegionRef,
    parentPassengerShardRegionRef,parentTimeInfoShardRegionRef,costAggregatorActor,timeAggregatorActor), "parentTaxiActor")

  import kantan.csv._
  import kantan.csv.ops._ // Automatic derivation of codecs.
  implicit val decoder: RowDecoder[TaxiTripEntry] = RowDecoder.ordered(TaxiTripEntry.apply _)
  //Start Processing including reading of the file
  val startTimeMillis = System.currentTimeMillis()
  //val source_csv = Source.fromResource("smallset.csv").mkString
  //val source_csv = Source.fromResource("100ksample.csv").mkString
  val source_csv = Source.fromResource("1ksample.csv").mkString
  //val source_csv = Source.fromResource("1millSample.csv").mkString
  val reader = source_csv.asCsvReader[TaxiTripEntry](rfc)

  //Give time for cluster to start up
  Thread.sleep(60000)
  import TaxiTripCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
  //Data loading:
  reader.foreach(either => {
    taxiTripActor ! CreateTaxiTripCommand((either.right.getOrElse(TaxiTripEntry(
      2,"2015-01-15 19:05:39","2015-01-15 19:23:42",1,1.59,-73.993896484375,40.750110626220703,1,"N",-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05
    ))))
  })

  //Operations testing

  taxiTripActor ! PrintTimeToLoad(startTimeMillis)
  //cb17c9a7-31d1-4863-ab1f-11bb7ed115f5
  taxiTripActor ! GetTaxiTripCost("2d4c8a0d-948f-4d69-a438-57c54abb2a84")






  /*
  reader.foreach(either => {
    persistentTaxiStatActor ! CreateTaxiStat((either.right.getOrElse(1)))
  }) */

  /*
   val quickListTest : List[String] = List(
     "2,2015-01-15 19:05:39,2015-01-15 19:23:42,1,1.59,-73.993896484375,40.750110626220703,1,N,-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05",
     "1,2015-01-10 20:33:38,2015-01-10 20:53:28,1,3.30,-74.00164794921875,40.7242431640625,1,N,-73.994415283203125,40.759109497070313,1,14.5,0.5,0.5,2,0,0.3,17.8",
     "1,2015-01-10 20:33:38,2015-01-10 20:43:41,1,1.80,-73.963340759277344,40.802787780761719,1,N,-73.951820373535156,40.824413299560547,2,9.5,0.5,0.5,0,0,0.3,10.8",
     "1,2015-01-10 20:33:39,2015-01-10 20:35:31,1,.50,-74.009086608886719,40.713817596435547,1,N,-74.004325866699219,40.719985961914063,2,3.5,0.5,0.5,0,0,0.3,4.8",
     "1,2015-01-10 20:33:39,2015-01-10 20:52:58,1,3.00,-73.971176147460938,40.762428283691406,1,N,-74.004180908203125,40.742652893066406,2,15,0.5,0.5,0,0,0.3,16.3",
     "1,2015-01-10 20:33:39,2015-01-10 20:53:52,1,9.00,-73.874374389648438,40.7740478515625,1,N,-73.986976623535156,40.758193969726563,1,27,0.5,0.5,6.7,5.33,0.3,40.33",
     "1,2015-01-10 20:33:39,2015-01-10 20:58:31,1,2.20,-73.9832763671875,40.726009368896484,1,N,-73.992469787597656,40.7496337890625,2,14,0.5,0.5,0,0,0.3,15.3",
   )*/



}

object RandomTest extends App {
  val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:SS")
  val d1 = format.parse("2015-01-10 20:58:31")
  val d2 = format.parse("2015-01-10 21:58:31")
  println(d1)
  println(d2)

  println((d2.getTime - d1.getTime)/60000)
}