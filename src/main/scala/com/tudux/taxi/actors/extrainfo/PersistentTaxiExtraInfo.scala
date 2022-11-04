package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse

case class TaxiTripExtraInfo(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                             storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double, deletedFlag: Boolean = false)

sealed trait TaxiExtraInfoCommand
object TaxiTripExtraInfoCommand {
  case class CreateTaxiTripExtraInfo(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoCommand
  case class GetTaxiTripExtraInfo(tripId: String) extends TaxiExtraInfoCommand
  case class UpdateTaxiTripExtraInfo(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoCommand
  case class DeleteTaxiTripExtraInfo(tripId: String) extends TaxiExtraInfoCommand

}


sealed trait TaxiExtraInfoEvent
object TaxiExtraInfoStatEvent{
  case class TaxiTripExtraInfoCreatedEvent(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoEvent
  case class TaxiTripExtraInfoUpdatedEvent(tripId: String, taxiExtraInfoStat: TaxiTripExtraInfo) extends TaxiExtraInfoEvent
  case class DeletedTaxiTripExtraInfoEvent(tripId: String) extends TaxiExtraInfoEvent
}

object ExtraInfoActorShardingSettings {
  import TaxiTripExtraInfoCommand._

  val numberOfShards = 10 //  use 10x number of nodes in your cluster
  val numberOfEntities = 100 // 10x number of shards
  // this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripExtraInfo@CreateTaxiTripExtraInfo(tripId,_) =>
      val entityId = tripId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripExtraInfo)
    case msg@GetTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@DeleteTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripExtraInfo(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  // this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripExtraInfo(tripId,_) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case DeleteTaxiTripExtraInfo(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripExtraInfo(statId,_) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}
object PersistentTaxiExtraInfo {
  def props: Props = Props(new PersistentTaxiExtraInfo)
}
class PersistentTaxiExtraInfo extends PersistentActor with ActorLogging {

  import TaxiTripExtraInfoCommand._
  import TaxiExtraInfoStatEvent._

  // Persistent Actor State
  // var statExtraInfoMap : Map[String,TaxiExtraInfoStat] = Map.empty
  var state: TaxiTripExtraInfo = TaxiTripExtraInfo(0,0,0,"",0,0)

  // override def persistenceId: String = id
  override def persistenceId: String = "ExtraInfo" + "-" + context.parent.path.name + "-" + self.path.name

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
    case CreateTaxiTripExtraInfo(tripId,taxiExtraInfoStat) =>
      // throw new RuntimeException("Mock Actor Failure") // Simulate Actor failure
      persist(TaxiTripExtraInfoCreatedEvent(tripId,taxiExtraInfoStat)) { _ =>
        log.info(s"Creating Extra Info Stat $taxiExtraInfoStat")
        state = taxiExtraInfoStat
        sender() ! OperationResponse(tripId,Right("Success"))

      }
    case UpdateTaxiTripExtraInfo(tripId,taxiExtraInfoStat) =>
      log.info(s"Updating Extra Info Stat $taxiExtraInfoStat")
      persist(TaxiTripExtraInfoUpdatedEvent(tripId, taxiExtraInfoStat)) { _ =>
        state = taxiExtraInfoStat
        sender() ! OperationResponse(tripId,Right("Success"))
      }

    case GetTaxiTripExtraInfo(tripId) =>
      log.info(s"Request to return Extra Info for tripId: $tripId")
      sender() ! state
    case DeleteTaxiTripExtraInfo(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripExtraInfoEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
        sender() ! OperationResponse(tripId,Right("Success"))
      }
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripExtraInfoCreatedEvent(tripId,taxiExtraInfoStat) =>
      log.info(s"Recovering Extra Info Stat $tripId")
      state = taxiExtraInfoStat
    case TaxiTripExtraInfoUpdatedEvent(tripId,taxiExtraInfoStat) =>
      log.info(s"Recovering Updated Extra Info Stat $tripId")
      state = taxiExtraInfoStat
    case DeletedTaxiTripExtraInfoEvent(tripId) =>
      log.info(s"Recovering Deleted Extra Info Stat for $tripId")
      state = state.copy(deletedFlag = true)

  }
}


