package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand.{AddTimeAggregatorValues, UpdateTimeAggregatorValues}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class TaxiTripTimeInfo(tpepPickupDatetime: String, tpepDropoffDatetime: String, deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoCommand {
  case class CreateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoCommand
  case class GetTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
  // TODO: Typed safety for optional actorref
  case class UpdateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo, timeAggregator: ActorRef = ActorRef.noSender) extends TaxiTripTimeInfoCommand
  case class DeleteTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoCreatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoUpdatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class DeletedTaxiTripTimeInfoEvent(tripId: String) extends TaxiTripTimeInfoEvent
}

object TimeInfoActorShardingSettings {

  import TaxiTripTimeInfoCommand._

  val numberOfShards = 1000 //  use 10x number of nodes in your cluster
  val numberOfEntities = 10000 // 10x number of shards
  // this help to map the corresponding message to a respective entity
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

  // this help to map the corresponding message to a respective shard
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

  import akka.pattern.{ask, pipe}
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  var state: TaxiTripTimeInfo = TaxiTripTimeInfo("","")

  // override def persistenceId: String = id
  override def persistenceId: String = "TimeInfo" + "-" + context.parent.path.name + "-" + self.path.name

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error("persist failure being triggered")
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage))
    super.onPersistFailure(cause, event, seqNr)
  }

  override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
    log.error("persist rejected being triggered")
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage))
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveCommand: Receive = {
    case CreateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat) =>
      persist(TaxiTripTimeInfoCreatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        log.info(s"Creating Trip Time Info Stat $taxiTripTimeInfoStat")
        state = taxiTripTimeInfoStat
        (timeAggregator ? AddTimeAggregatorValues(tripId,taxiTripTimeInfoStat)).pipeTo(sender())
        // sender() ! OperationResponse(tripId,Right("Success"))
      }
    case UpdateTaxiTripTimeInfo(tripId, taxiTripTimeInfoStat, _) =>
      log.info("Updating Time Info ")
      persist(TaxiTripTimeInfoUpdatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        state = taxiTripTimeInfoStat
        (timeAggregator ? UpdateTimeAggregatorValues(tripId,state,taxiTripTimeInfoStat)).pipeTo(sender())
        // sender() ! OperationResponse(tripId,Right("Success"))
      }
    case GetTaxiTripTimeInfo(tripId) =>
      log.info(s"Request to return Time Info for tripId: $tripId")
      sender() ! state
    case DeleteTaxiTripTimeInfo(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripTimeInfoEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
        sender() ! OperationResponse(tripId,Right("Success"))
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


