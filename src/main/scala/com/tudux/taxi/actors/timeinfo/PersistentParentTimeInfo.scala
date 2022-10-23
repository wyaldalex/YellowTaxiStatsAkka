package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripCommand, TaxiTripEvent, TaxiTripResponse}


object TimeInfoActorShardingSettings {
  import TaxiTripCommand._
  import TaxiTripTimeInfoCommand._

  val numberOfShards = 10 // use 10x number of nodes in your cluster
  val numberOfEntities = 100 //10x number of shards
  //this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat, statId) =>
      val entityId = statId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripCommand)
    case msg@GetTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripTimeInfo(statId,_,_) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  //this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripCommand(taxiStat, statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripTimeInfo(statId,_,_) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}

object PersistentParentTimeInfo {
  def props(id: String, timeAggregator: ActorRef) : Props = Props(new PersistentParentTimeInfo(id,timeAggregator))

  case class TaxiTripTimeInfoState(
                           timeinfo: Map[String, ActorRef]
                          )
}
class PersistentParentTimeInfo(id: String,timeAggregator: ActorRef) extends PersistentActor with ActorLogging {

  import PersistentParentTimeInfo._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._
  import TaxiTripTimeInfoCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._

  var state: TaxiTripTimeInfoState = TaxiTripTimeInfoState(Map.empty)

  override def preStart(): Unit = {
    super.preStart()
    log.info("Sharded Parent Time Info Started")
  }
  
  def createTimeInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripTimeInfo.props(id), id)
  }

  //override def persistenceId: String = id
  override def persistenceId: String = "TimeInfo" + "-" + context.parent.path.name + "-" + self.path.name;

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiTimeInfoActor = createTimeInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(timeinfo = state.timeinfo + (statId -> newTaxiTimeInfoActor))

        newTaxiTimeInfoActor ! CreateTaxiTripTimeInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    //Individual Gets
    case getTaxiTimeInfoStat@GetTaxiTripTimeInfo(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor.forward(getTaxiTimeInfoStat)
    //Individual Updates
    case UpdateTaxiTripTimeInfo(statId, taxiTripTimeInfoStat, _) =>
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor.forward(UpdateTaxiTripTimeInfo(statId, taxiTripTimeInfoStat, timeAggregator))
    //General Delete
    case DeleteTaxiTrip(statId) =>
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor ! DeleteTaxiTripTimeInfo(statId)
    //Individual Stats
    case GetTotalTimeInfoInfoLoaded =>
        log.info("Returning total time info loaded size")
        sender() ! state.timeinfo.size
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
      val timeInfoActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiTripTimeInfo.props(statId), statId))
      state = state.copy(timeinfo = state.timeinfo + (statId -> timeInfoActor))
      
  }

}
