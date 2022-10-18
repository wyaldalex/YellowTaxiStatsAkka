package com.tudux.taxi.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.{PersistentCostStatsAggregator, PersistentTimeStatsAggregator}
import com.tudux.taxi.actors.cost.PersistentParentTaxiCost
import com.tudux.taxi.actors.extrainfo.PersistentParentExtraInfo
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.passenger.PersistentParentPassengerInfo
import com.tudux.taxi.actors.timeinfo.PersistentParentTimeInfo

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
  def props: Props = Props(new TaxiTripActor)

}
class TaxiTripActor extends Actor with ActorLogging {

  import com.tudux.taxi.actors.aggregators.CostAggregatorCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
  import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand._
  import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand._
  import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand._

  val costActorIdSuffix = "-cost"
  val extraInfoActorIdSuffix = "-extrainfo"
  val timeActorIdSuffix = "-time"
  val passengerActorIdSuffix = "-passenger"

  //Parent Actors
  val parentCostActor : ActorRef = context.actorOf(PersistentParentTaxiCost.props("parent-cost"), "parent-cost")
  val parentExtraInfoActor : ActorRef = context.actorOf(PersistentParentExtraInfo.props("parent-extrainfo"), "parent-extrainfo")
  val parentTimeInfoActor : ActorRef = context.actorOf(PersistentParentTimeInfo.props("parent-timeinfo"), "parent-timeinfo")
  val parentPassengerInfo : ActorRef = context.actorOf(PersistentParentPassengerInfo.props("parent-passengerinfo"), "parent-passengerinfo")

  //Aggregators
  val costAggregatorActor : ActorRef = context.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")
  val timeAggregatorActor : ActorRef = context.actorOf(PersistentTimeStatsAggregator.props("time-aggregator"), "time-aggregator")

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
      parentCostActor ! CreateTaxiTripCommand(taxiTrip,tripId.concat(costActorIdSuffix))
      parentExtraInfoActor ! CreateTaxiTripCommand(taxiTrip,tripId.concat(extraInfoActorIdSuffix))
      parentPassengerInfo ! CreateTaxiTripCommand(taxiTrip,tripId.concat(passengerActorIdSuffix))
      parentTimeInfoActor ! CreateTaxiTripCommand(taxiTrip,tripId.concat(timeActorIdSuffix))
      costAggregatorActor ! AddCostAggregatorValues(tripId,taxiTrip)
      timeAggregatorActor ! AddTimeAggregatorValues(taxiTrip)

    sender() ! TaxiTripCreatedResponse(tripId)

    //Individual Gets
    case GetTaxiTripCost(tripId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      parentCostActor.forward(GetTaxiTripCost(tripId.concat(costActorIdSuffix)))
    case GetTaxiTripExtraInfo(tripId) =>
      log.info(s"Receive Taxi Extra Info Inquiry, forwarding")
      parentExtraInfoActor.forward(GetTaxiTripExtraInfo(tripId.concat(extraInfoActorIdSuffix)))
    case GetTaxiTripTimeInfo(tripId) =>
      log.info(s"Receive Taxi Time Info Inquiry, forwarding")
      parentTimeInfoActor.forward(GetTaxiTripTimeInfo(tripId.concat(timeActorIdSuffix)))
    case GetTaxiTripPassengerInfo(tripId) =>
      log.info(s"Receive Taxi Passenger Info Inquiry, forwarding")
      parentPassengerInfo.forward(GetTaxiTripPassengerInfo(tripId.concat(passengerActorIdSuffix)))
    //Individual Updates
    case UpdateTaxiTripPassenger(tripId,taxiTripPassengerInfoStat) =>
      log.info(s"Received Taxi Passenger Info request for $tripId")
      parentPassengerInfo.forward(UpdateTaxiTripPassenger(tripId.concat(passengerActorIdSuffix), taxiTripPassengerInfoStat))
    case UpdateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat,_) =>
      log.info(s"Received Taxi Passenger Info request for $tripId")
      parentTimeInfoActor.forward(UpdateTaxiTripTimeInfo(tripId.concat(timeActorIdSuffix), taxiTripTimeInfoStat, timeAggregatorActor))
    case UpdateTaxiTripExtraInfo(tripId,taxiExtraInfoStat) =>
      log.info(s"Received Taxi Passenger Info request for $tripId")
      parentExtraInfoActor.forward(UpdateTaxiTripExtraInfo(tripId.concat(extraInfoActorIdSuffix),taxiExtraInfoStat))
    case UpdateTaxiTripCost(tripId,taxiCostStat,_) =>
      log.info(s"Received Taxi Passenger Info request for $tripId")
       parentCostActor.forward(UpdateTaxiTripCost(tripId.concat(costActorIdSuffix), taxiCostStat , costAggregatorActor))
    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(tripId) =>
      parentCostActor ! DeleteTaxiTrip(tripId.concat(costActorIdSuffix))
      parentExtraInfoActor ! DeleteTaxiTrip(tripId.concat(extraInfoActorIdSuffix))
      parentPassengerInfo ! DeleteTaxiTrip(tripId.concat(passengerActorIdSuffix))
      parentTimeInfoActor ! DeleteTaxiTrip(tripId.concat(timeActorIdSuffix))
    //Domain Specific Operations
    case calculateTripDistanceCost@CalculateTripDistanceCost(_) =>
      log.info("Received CalculateTripDistanceCost request")
      costAggregatorActor.forward(calculateTripDistanceCost)
    case getAverageTripTime@GetAverageTripTime =>
      timeAggregatorActor.forward(getAverageTripTime)
    case getAverageTipAmount@GetAverageTipAmount =>
      log.info("Received GetAverageTipAmount request")
      costAggregatorActor.forward(getAverageTipAmount)
    //Individual Stats
    case getTotalCostLoaded@GetTotalCostLoaded =>
      parentCostActor.forward(getTotalCostLoaded)
    case getTotalExtraInfoLoaded@GetTotalExtraInfoLoaded =>
      parentExtraInfoActor.forward(getTotalExtraInfoLoaded)
    case getTotalTimeInfoInfoLoaded@GetTotalTimeInfoInfoLoaded =>
      parentTimeInfoActor.forward(getTotalTimeInfoInfoLoaded)
    case getTotalPassengerInfoLoaded@GetTotalPassengerInfoLoaded =>
      parentPassengerInfo.forward(getTotalPassengerInfoLoaded)

    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
//      taxiTripCostActor.forward(printTimeToLoad)
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

}

object TaxiStatAppLoader extends App {

  implicit val system: ActorSystem = ActorSystem("TaxiLoader")
  implicit val timeout: Timeout = Timeout(2.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher
  /*
   val localStoreActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
   *///
  //val persistentTaxiStatActor = system.actorOf(PersistentTaxiStatActor.props, "quickPersistentActorTest")
  val taxiTripActor = system.actorOf(TaxiTripActor.props, "parentTaxiActor")

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