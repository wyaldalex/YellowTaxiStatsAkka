package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripResponse, TaxiTripCommand, TaxiTripEvent}


object ExtraInfoActorShardingSettings {
  import TaxiTripCommand._
  import TaxiTripExtraInfoCommand._

  val numberOfShards = 10 // use 10x number of nodes in your cluster
  val numberOfEntities = 100 //10x number of shards
  //this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat, statId) =>
      val entityId = statId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripCommand)
    case msg@GetTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripExtraInfo(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  //this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripCommand(taxiStat, statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripExtraInfo(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}
object PersistentParentExtraInfo {
  def props(id: String) : Props = Props(new PersistentParentExtraInfo(id))
  case class TaxiTripExtraInfoState(extrainfo: Map[String, ActorRef])
}
class PersistentParentExtraInfo(id: String) extends PersistentActor with ActorLogging  {

  import PersistentParentExtraInfo._
  import TaxiTripExtraInfoCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._

  override def preStart(): Unit = {
    super.preStart()
    log.info("Sharded Parent ExtraInfo Started")
  }

  var state: TaxiTripExtraInfoState = TaxiTripExtraInfoState(Map.empty)

  def createTaxiExtraInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiExtraInfo.props(id), id)
  }
  

  //override def persistenceId: String = id
  override def persistenceId: String = "ExtraInfo" + "-" + context.parent.path.name + "-" + self.path.name;

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      val newTaxiExtraInfoActor = createTaxiExtraInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(extrainfo = state.extrainfo + ((statId) -> newTaxiExtraInfoActor))

        newTaxiExtraInfoActor ! CreateTaxiTripExtraInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    case getTaxiExtraInfoStat@GetTaxiTripExtraInfo(statId) =>
      log.info(s"Receive Taxi ExtraInfo Inquiry, forwarding")
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor.forward(getTaxiExtraInfoStat)

    //Individual Updates

    case updateTaxiExtraInfoStat@UpdateTaxiTripExtraInfo(statId, _) =>
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor.forward(updateTaxiExtraInfoStat)

    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor ! DeleteTaxiTripExtraInfo(statId)

    //Individual Stats
    case GetTotalExtraInfoLoaded =>
      log.info("Returning total Extra info info loaded size")
      sender() ! state.extrainfo.size

    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }
  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip for id: $statId")
      val extraInfoActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiExtraInfo.props(statId), statId))
      state = state.copy(extrainfo = state.extrainfo + (statId -> extraInfoActor))

  }

}
