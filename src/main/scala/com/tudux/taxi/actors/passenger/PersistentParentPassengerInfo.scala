package com.tudux.taxi.actors.passenger

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.cost.TaxiTripCostCommand
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripCommand, TaxiTripEvent, TaxiTripResponse}


object PassengerInfoActorShardingSettings {
  import TaxiTripCommand._
  import TaxiTripPassengerInfoCommand._

  val numberOfShards = 10 // use 10x number of nodes in your cluster
  val numberOfEntities = 100 //10x number of shards
  //this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat, statId) =>
      val entityId = statId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripCommand)
    case msg@GetTaxiTripPassengerInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripPassenger(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  //this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripCommand(taxiStat, statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripPassengerInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripPassenger(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}
object PersistentParentPassengerInfo {
  def props(id: String): Props = Props(new PersistentParentPassengerInfo(id))

  case class TaxiTripPassengerInfoState(
                           passengerinfo: Map[String, ActorRef]
                          )
}
class PersistentParentPassengerInfo(id: String) extends PersistentActor with ActorLogging  {

  import PersistentParentPassengerInfo._
  import TaxiTripCostCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._
  import TaxiTripPassengerInfoCommand._


  override def preStart(): Unit = {
    super.preStart()
    log.info("Sharded Parent Passenger Info Started")
  }

  var state: TaxiTripPassengerInfoState = TaxiTripPassengerInfoState(Map.empty)

  def createPassengerInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripPassengerInfo.props(id), id)
  }


  //override def persistenceId: String = id
  override def persistenceId: String = "PassengerInfo" + "-" + context.parent.path.name + "-" + self.path.name;

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiPassengerInfoActor = createPassengerInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(passengerinfo = state.passengerinfo + (statId -> newTaxiPassengerInfoActor))

        newTaxiPassengerInfoActor ! CreateTaxiTripPassengerInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    case getTaxiPassengerInfoStat@GetTaxiTripPassengerInfo(statId) =>
      log.info(s"Receive Taxi Passenger Info Inquiry, forwarding")
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(getTaxiPassengerInfoStat)
    //Individual Updates
    case updateTaxiPassenger@UpdateTaxiTripPassenger(statId, _) =>
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(updateTaxiPassenger)

    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>

      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor ! DeleteTaxiTripPassenger(statId)

    //Individual Stats
    case GetTotalPassengerInfoLoaded =>
      log.info("Returning total passenger info loaded size")
      sender() ! state.passengerinfo.size

    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
    //      taxiTripCostActor.forward(printTimeToLoad)
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip for id: $statId")
      val passengerActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiTripPassengerInfo.props(statId), statId))
      state = state.copy(passengerinfo = state.passengerinfo + (statId -> passengerActor))

  }

}
