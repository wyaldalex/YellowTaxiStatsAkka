package com.tudux.taxi.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.tudux.taxi.actors.CostAggregatorCommand.AddCostAggregatorValues
import com.tudux.taxi.actors.TaxiStatResponseResponses.TaxiStatCreatedResponse

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.io.Source

sealed trait TaxiTripCommand
object TaxiTripCommand {
  case class CreateTaxiTripCommand(taxiStat: TaxiStat) extends  TaxiTripCommand
}
sealed trait TaxiTripEvent
object TaxiTripEvent {
  case class CreatedTaxiTripEvent(statId: String) extends  TaxiTripEvent
}

object TaxiTripActor {
  def props: Props = Props(new TaxiTripActor)

  case class TaxiTripState(costs: Map[String, ActorRef],
                           timeinfo: Map[String, ActorRef],
                           extrainfo: Map[String, ActorRef],
                           passengerinfo: Map[String, ActorRef]
                          )
}
class TaxiTripActor extends PersistentActor with ActorLogging {

  import TaxiCostStatCommand._
  import CostAggregatorCommand._
  import TaxiExtraInfoStatCommand._
  import TaxiStatCommand._
  import TaxiTripPassengerInfoStatCommand._
  import TaxiTripTimeInfoStatCommand._
  import TaxiTripActor._
  import TaxiTripCommand._
  import TaxiTripEvent._

  var state: TaxiTripState = TaxiTripState(Map.empty,Map.empty,Map.empty,Map.empty )
  val costActorIdSuffix = "-cost"
  val extraInfoActorIdSuffix = "-extrainfo"
  val timeActorIdSuffix = "-time"
  val passengerActorIdSuffix = "-passenger"

  val costAggregatorActor : ActorRef = context.actorOf(PersistentCostStatsAggregator.props("cost-aggregator"), "cost-aggregator")

  implicit def toTaxiCost(taxiStat: TaxiStat) : TaxiCostStat = {
    TaxiCostStat(taxiStat.VendorID, taxiStat.trip_distance,
      taxiStat.payment_type, taxiStat.fare_amount, taxiStat.fare_amount, taxiStat.mta_tax,
      taxiStat.tip_amount, taxiStat.tolls_amount, taxiStat.improvement_surcharge, taxiStat.total_amount)
  }

  implicit def toTaxiExtraInfo(taxiStat: TaxiStat): TaxiExtraInfoStat = {
    TaxiExtraInfoStat(taxiStat.pickup_longitude, taxiStat.pickup_latitude,
      taxiStat.RateCodeID, taxiStat.store_and_fwd_flag, taxiStat.dropoff_longitude, taxiStat.dropoff_latitude)
  }

  implicit def toTaxiPassengerInfo(taxiStat: TaxiStat): TaxiTripPassengerInfoStat = {
    TaxiTripPassengerInfoStat(taxiStat.passenger_count)
  }

  implicit def toTaxiTimeInfoStat(taxiStat: TaxiStat): TaxiTripTimeInfoStat = {
    TaxiTripTimeInfoStat(taxiStat.tpep_pickup_datetime, taxiStat.tpep_dropoff_datetime)
  }

  implicit def toAggregatorStat(taxiStat: TaxiStat) : AggregatorStat = {
    AggregatorStat(taxiStat.total_amount, taxiStat.trip_distance, taxiStat.tip_amount)
  }



  //var idStat : Int = 1

  //split main Taxi payload into different actors
  //private val taxiTripCostActor: ActorRef = createTaxiCostActor()
  //private val taxiExtraInfoActor: ActorRef = createTaxiExtraInfoActor()
  //private val taxiPassengerInfoActor: ActorRef = createPassengerInfoActor()
  //private val taxiTimeInfoActor: ActorRef = createTimeInfoActor()

  def createTaxiCostActor(id: String) : ActorRef = {
    context.actorOf(PersistentTaxiTripCost.props(id), id)
  }

  def createTaxiExtraInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiExtraInfo.props(id), id)
  }

  def createPassengerInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripPassengerInfo.props(id), id)
  }

  def createTimeInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripTimeInfo.props(id), id)
  }

  override def persistenceId: String = "taxiTripParentActor"

  override def receiveCommand : Receive = {
    case CreateTaxiTripCommand(taxiStat) =>
      //generate new stat ID to avoid conflicts
      val statId = UUID.randomUUID().toString
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiTripCostActor = createTaxiCostActor(statId.concat(costActorIdSuffix))
      val newTaxiExtraInfoActor = createTaxiExtraInfoActor(statId.concat(extraInfoActorIdSuffix))
      val newTaxiPassengerInfoActor = createPassengerInfoActor(statId.concat(passengerActorIdSuffix))
      val newTaxiTimeInfoActor = createTimeInfoActor(statId.concat(timeActorIdSuffix))
      /*
      new state modification
       */
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(costs = state.costs + (statId.concat(costActorIdSuffix) -> newTaxiTripCostActor))
        state = state.copy(extrainfo = state.extrainfo + ((statId + extraInfoActorIdSuffix) -> newTaxiExtraInfoActor))
        state = state.copy(passengerinfo = state.passengerinfo + ((statId + passengerActorIdSuffix) -> newTaxiPassengerInfoActor))
        state = state.copy(timeinfo = state.timeinfo + ((statId + timeActorIdSuffix) -> newTaxiTimeInfoActor))

        newTaxiTripCostActor ! CreateTaxiCostStat(statId,taxiStat)
        newTaxiExtraInfoActor ! CreateTaxiExtraInfoStat(statId,taxiStat)
        newTaxiPassengerInfoActor ! CreateTaxiTripPassengerInfoStat(statId,taxiStat)
        newTaxiTimeInfoActor ! CreateTaxiTripTimeInfoStat(statId,taxiStat)
        costAggregatorActor ! AddCostAggregatorValues(statId,taxiStat)

      }

      sender() ! TaxiStatCreatedResponse(statId)

    case GetTotalTaxiCostStats =>
      log.info("To be implemented")
      //candidate to be a forward operation
      //taxiTripCostActor ! GetTotalTaxiCostStats
      log.info(s"Received petition to return size which is: ${state.costs.size})")
    //Individual Gets
    case getTaxiCostStat@GetTaxiCostStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      //taxiTripCostActor.forward(getTaxiCostStat)
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor.forward(getTaxiCostStat)
    case getTaxiExtraInfoStat@GetTaxiExtraInfoStat(_) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      //taxiExtraInfoActor.forward(getTaxiExtraInfoStat)
    case getTaxiTimeInfoStat@GetTaxiTimeInfoStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      //taxiTimeInfoActor.forward(getTaxiTimeInfoStat)
    case getTaxiPassengerInfoStat@GetTaxiPassengerInfoStat(_) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      //taxiPassengerInfoActor.forward(getTaxiPassengerInfoStat)
    //Individual Updates
    case updateTaxiPassenger@UpdateTaxiPassenger(_,_) =>
      log.info("To be implemented")
      //taxiPassengerInfoActor.forward(updateTaxiPassenger)
    case updateTaxiTripTimeInfoStat@UpdateTaxiTripTimeInfoStat(_,_) =>
      log.info("To be implemented")
      //taxiTimeInfoActor.forward(updateTaxiTripTimeInfoStat)
    case updateTaxiExtraInfoStat@UpdateTaxiExtraInfoStat(_,_) =>
      log.info("To be implemented")
      //taxiExtraInfoActor.forward(updateTaxiExtraInfoStat)
    case updateTaxiCostStat@UpdateTaxiCostStat(_,_) =>
      log.info("To be implemented")
      //taxiTripCostActor.forward(updateTaxiCostStat)
    //General Delete
    case DeleteTaxiStat(statId) =>
      log.info("To be implemented")
//      taxiTripCostActor ! DeleteTaxiCostStat(statId)
//      taxiExtraInfoActor ! DeleteTaxiExtraInfo(statId)
//      taxiPassengerInfoActor ! DeleteTaxiTripPassenger(statId)
//      taxiTimeInfoActor ! DeleteTaxiTripTimeInfoStat(statId)
    //Domain Specific Operations
    case calculateTripDistanceCost@CalculateTripDistanceCost(_) =>
      log.info("Received CalculateTripDistanceCost request")
      costAggregatorActor.forward(calculateTripDistanceCost)
    case getAverageTripTime@GetAverageTripTime =>
      log.info("To be implemented")
      //taxiTimeInfoActor.forward(getAverageTripTime)
    case getAverageTipAmount@GetAverageTipAmount =>
      log.info("Received GetAverageTipAmount request")
      costAggregatorActor.forward(getAverageTipAmount)
    //Individual Stats
    case getTotalCostLoaded@GetTotalCostLoaded =>
      log.info("To be implemented")
//      taxiTripCostActor.forward(getTotalCostLoaded)
    case getTotalExtraInfoLoaded@GetTotalExtraInfoLoaded =>
      log.info("To be implemented")
//      taxiExtraInfoActor.forward(getTotalExtraInfoLoaded)
    case getTotalTimeInfoInfoLoaded@GetTotalTimeInfoInfoLoaded =>
      log.info("To be implemented")
//      taxiTimeInfoActor.forward(getTotalTimeInfoInfoLoaded)
    case getTotalPassengerInfoLoaded@GetTotalPassengerInfoLoaded =>
      log.info("To be implemented")
//      taxiPassengerInfoActor.forward(getTotalPassengerInfoLoaded)

    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
//      taxiTripCostActor.forward(printTimeToLoad)
    case _ => log.info("Received something else at parent actor")

  }

  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip for id: $statId")
      val costActor = context.child(statId.concat(costActorIdSuffix))
        .getOrElse(context.actorOf(PersistentTaxiTripCost.props(statId.concat(costActorIdSuffix)), statId.concat(costActorIdSuffix)))
      state = state.copy(costs = state.costs + (statId -> costActor))

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
  implicit val decoder: RowDecoder[TaxiStat] = RowDecoder.ordered(TaxiStat.apply _)
  //Start Processing including reading of the file
  val startTimeMillis = System.currentTimeMillis()
  val source_csv = Source.fromResource("smallset.csv").mkString
  //val source_csv = Source.fromResource("100ksample.csv").mkString
  //val source_csv = Source.fromResource("1ksample.csv").mkString
  //val source_csv = Source.fromResource("1millSample.csv").mkString
  val reader = source_csv.asCsvReader[TaxiStat](rfc)

  import TaxiCostStatCommand._
  import TaxiStatCommand._
  import TaxiTripCommand._
  //Data loading:
  reader.foreach(either => {
    taxiTripActor ! CreateTaxiTripCommand((either.right.getOrElse(TaxiStat(
      2,"2015-01-15 19:05:39","2015-01-15 19:23:42",1,1.59,-73.993896484375,40.750110626220703,1,"N",-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05
    ))))
  })

  //Operations testing

  taxiTripActor ! PrintTimeToLoad(startTimeMillis)
  taxiTripActor ! GetTotalTaxiCostStats
  //cb17c9a7-31d1-4863-ab1f-11bb7ed115f5
  taxiTripActor ! GetTaxiCostStat("2d4c8a0d-948f-4d69-a438-57c54abb2a84")






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