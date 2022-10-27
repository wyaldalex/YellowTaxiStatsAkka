package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand.{AddTimeAggregatorValues, UpdateTimeAggregatorValues}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse

case class TaxiTripTimeInfo(tpepPickupDatetime: String, tpepDropoffDatetime: String, deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoCommand {
  case class CreateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoCommand
  case class GetTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
  case class UpdateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo, timeAggregator: ActorRef = ActorRef.noSender) extends TaxiTripTimeInfoCommand
  case class DeleteTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
  case object GetTotalTimeInfoInfoLoaded
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoCreatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoUpdatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class DeletedTaxiTripTimeInfoEvent(tripId: String) extends TaxiTripTimeInfoEvent
}

sealed trait TaxiTripTimeResponse
object TaxiTripTimeResponse {
  case class TaxiTripTimeResponseCreated(id: String)
}



object TimeInfoActorShardingSettings {

  import TaxiTripTimeInfoCommand._

  val numberOfShards = 10 // use 10x number of nodes in your cluster
  val numberOfEntities = 100 //10x number of shards
  //this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripTimeInfo@CreateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat) =>
      val entityId = tripId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripTimeInfo)
    case msg@GetTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@DeleteTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripTimeInfo(statId,_,_) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  //this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case DeleteTaxiTripTimeInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripTimeInfo(statId,_,_) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}
object PersistentTaxiTripTimeInfo {
  def props(timeAggregator: ActorRef): Props = Props(new PersistentTaxiTripTimeInfo(timeAggregator))
}
class PersistentTaxiTripTimeInfo(timeAggregator: ActorRef) extends PersistentActor with ActorLogging {

  import TaxiTripTimeInfoCommand._
  import TaxiTripTimeInfoStatEvent._
  import TaxiTripTimeResponse._


  var state : TaxiTripTimeInfo = TaxiTripTimeInfo("","")

  //override def persistenceId: String = id
  override def persistenceId: String = "TimeInfo" + "-" + context.parent.path.name + "-" + self.path.name

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    sender() ! OperationResponse("", "Failure", cause.getMessage)
    super.onPersistFailure(cause, event, seqNr)
  }

  override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
    sender() ! OperationResponse("", "Failure", cause.getMessage)
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveCommand: Receive = {
    case CreateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat) =>
      persist(TaxiTripTimeInfoCreatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        log.info(s"Creating Trip Time Info Stat $taxiTripTimeInfoStat")
        state = taxiTripTimeInfoStat
        timeAggregator ! AddTimeAggregatorValues(taxiTripTimeInfoStat)
        sender() ! OperationResponse(tripId)
      }
    case UpdateTaxiTripTimeInfo(tripId, taxiTripTimeInfoStat, _) =>
      log.info("Updating Time Info ")
      persist(TaxiTripTimeInfoUpdatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        timeAggregator ! UpdateTimeAggregatorValues(state,taxiTripTimeInfoStat)
        state = taxiTripTimeInfoStat
        sender() ! OperationResponse(tripId)
      }
    case GetTaxiTripTimeInfo(_) =>
      sender() ! state
    case DeleteTaxiTripTimeInfo(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripTimeInfoEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
        sender() ! OperationResponse(tripId)
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripTimeInfoCreatedEvent(tripId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Trip Time Info Stat $tripId")
      state = taxiTripTimeInfoStat

    case TaxiTripTimeInfoUpdatedEvent(tripId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Update Trip Time Info Stat $tripId")
      state = taxiTripTimeInfoStat

    case DeletedTaxiTripTimeInfoEvent(tripId) =>
      log.info(s"Recovering Deleted Trip Time Info Stat $tripId")
      state = state.copy(deletedFlag = true)


  }
}


