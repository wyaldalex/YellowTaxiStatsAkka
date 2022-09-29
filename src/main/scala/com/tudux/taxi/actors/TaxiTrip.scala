package com.tudux.taxi.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiStatResponseResponses.TaxiStatCreatedResponse

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.concurrent.duration._

object TaxiTripActor {
  def props: Props = Props(new TaxiTripActor)
}

class TaxiTripActor extends Actor with ActorLogging {

  import TaxiStatCommand._
  import TaxiCostStatCommand._
  import TaxiExtraInfoStatCommand._
  import TaxiTripPassengerInfoStatCommand._
  import TaxiTripTimeInfoStatCommand._

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

  //var idStat : Int = 1

  //split main Taxi payload into different actors
  private val taxiTripCostActor: ActorRef = createTaxiCostActor()
  private val taxiExtraInfoActor: ActorRef = createTaxiExtraInfoActor()
  private val taxiPassengerInfoActor: ActorRef = createPassengerInfoActor()
  private val taxiTimeInfoActor: ActorRef = createTimeInfoActor()

  def createTaxiCostActor() : ActorRef = {
    val taxiTripActorId = "persistent-taxi-cost-stats-actor"
    context.actorOf(PersistentTaxiTripCost.props(taxiTripActorId), taxiTripActorId)
  }

  def createTaxiExtraInfoActor(): ActorRef = {
    val taxiTripActorId = "persistent-taxi-extra-info-stats-actor"
    context.actorOf(PersistentTaxiExtraInfo.props(taxiTripActorId), taxiTripActorId)
  }

  def createPassengerInfoActor(): ActorRef = {
    val taxiTripActorId = "persistent-passenger-info-stats-actor"
    context.actorOf(PersistentTaxiTripPassengerInfo.props(taxiTripActorId), taxiTripActorId)
  }

  def createTimeInfoActor(): ActorRef = {
    val taxiTripActorId = "persistent-time-info-stats-actor"
    context.actorOf(PersistentTaxiTripTimeInfo.props(taxiTripActorId), taxiTripActorId)
  }

  /*
  override def receive: Receive = {
    case CreateTaxiStat(taxiStat) =>
      log.info(s"Received $taxiStat to create")
      taxiTripCostActor.forward(CreateTaxiStat(taxiStat))
    case _ => log.info("Received something else")
  } */
  override def receive: Receive = {
    case CreateTaxiStat(taxiStat) =>
      //generate new stat ID to avoid conflicts
      val idStat = UUID.randomUUID().toString
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      taxiTripCostActor  ! CreateTaxiCostStat(idStat,taxiStat)
      taxiExtraInfoActor ! CreateTaxiExtraInfoStat(idStat,taxiStat)
      taxiPassengerInfoActor ! CreateTaxiTripPassengerInfoStat(idStat,taxiStat)
      taxiTimeInfoActor ! CreateTaxiTripTimeInfoStat(idStat,taxiStat)
      sender() ! TaxiStatCreatedResponse(idStat)
    case GetTotalTaxiCostStats =>
      //candidate to be a forward operation
      taxiTripCostActor ! GetTotalTaxiCostStats
    case GetTaxiCostStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      taxiTripCostActor.forward(GetTaxiCostStat(statId))
    case GetTaxiExtraInfoStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      taxiExtraInfoActor.forward(GetTaxiExtraInfoStat(statId))
    case GetTaxiTimeInfoStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      taxiTimeInfoActor.forward(GetTaxiTimeInfoStat(statId))
    case GetTaxiPassengerInfoStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      taxiPassengerInfoActor.forward(GetTaxiPassengerInfoStat(statId))

    case _ => log.info("Received something else")
  }

}


object TaxiStatApp extends App {

  implicit val system: ActorSystem = ActorSystem("BankPlayground")
  implicit val timeout: Timeout = Timeout(2.seconds)
  implicit val scheduler: ExecutionContext = system.dispatcher
  /*
   val localStoreActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandraDemo"))
   *///
  //val persistentTaxiStatActor = system.actorOf(PersistentTaxiStatActor.props, "quickPersistentActorTest")
  val taxiTripActor = system.actorOf(TaxiTripActor.props, "parentTaxiActor")


  import kantan.csv._ // All kantan.csv types.
  import kantan.csv.ops._ // Enriches types with useful methods.
  import kantan.csv.generic._ // Automatic derivation of codecs.
  implicit val decoder: RowDecoder[TaxiStat] = RowDecoder.ordered(TaxiStat.apply _)
  val source_csv = Source.fromResource("smallset.csv").mkString
  val reader = source_csv.asCsvReader[TaxiStat](rfc)

  import TaxiStatEvent._
  import TaxiStatCommand._
  import TaxiCostStatCommand._
  //Data loading:
//  reader.foreach(either => {
//    //println(either.right.getOrElse(1))
//    taxiTripActor ! CreateTaxiStat((either.right.getOrElse(TaxiStat(
//      2,"2015-01-15 19:05:39","2015-01-15 19:23:42",1,1.59,-73.993896484375,40.750110626220703,1,"N",-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05
//    ))))
//  })
  //Operations testing
  taxiTripActor ! GetTotalTaxiCostStats

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