package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.tudux.taxi.actors.aggregators.AggregatorStat
import com.tudux.taxi.actors.aggregators.CostAggregatorCommand.{AddCostAggregatorValues, UpdateCostAggregatorValues}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

case class TaxiTripCost(vendorID: Int,
                        tripDistance: Double,
                        paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                        tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double, deletedFlag: Boolean = false)


sealed trait TaxiCostCommand
object TaxiTripCostCommand {
  case class CreateTaxiTripCost(tripId: String, taxiTripCost: TaxiTripCost) extends TaxiCostCommand
  case class GetTaxiTripCost(tripId: String) extends  TaxiCostCommand
  case class UpdateTaxiTripCost(tripId: String, taxiTripCost: TaxiTripCost, costAggregator: ActorRef = ActorRef.noSender) extends TaxiCostCommand
  case class DeleteTaxiTripCost(tripId: String) extends TaxiCostCommand
}


sealed trait TaxiCostEvent
object TaxiTripCostEvent{
  case class TaxiTripCostCreatedEvent(tripId: String, taxiTripCost: TaxiTripCost // , migrationError: String = "Cause Failure"
                                     ) extends TaxiCostEvent
  case class UpdatedTaxiTripCostEvent(tripId: String, taxiTripCost: TaxiTripCost) extends TaxiCostEvent
  case class DeletedTaxiTripCostEvent(tripId: String) extends TaxiCostEvent
}

object CostActorShardingSettings {

  import TaxiTripCostCommand._

  val numberOfShards = 10 //  use 10x number of nodes in your cluster
  val numberOfEntities = 100 // 10x number of shards
  // this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripCost@CreateTaxiTripCost(tripId,_) =>
      val entityId = tripId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripCost)
    case msg@GetTaxiTripCost(tripId) =>
      val shardId = tripId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@DeleteTaxiTripCost(tripId) =>
      val shardId = tripId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
    case msg@UpdateTaxiTripCost(tripId,_,_) =>
      val shardId = tripId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  // this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripCost(tripId,_) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripCost(tripId) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case DeleteTaxiTripCost(tripId) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case UpdateTaxiTripCost(tripId,_, _) =>
      val shardId = tripId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}
object PersistentTaxiTripCost {
  def props(costAggregator: ActorRef): Props = Props(new PersistentTaxiTripCost(costAggregator))
}
class PersistentTaxiTripCost(costAggregator: ActorRef) extends PersistentActor with ActorLogging {
// Scala vs Golang,NodeJs
  import TaxiTripCostCommand._
  import TaxiTripCostEvent._

  import akka.pattern.{ask, pipe}
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5 seconds)

  var state : TaxiTripCost = TaxiTripCost(0,0,0,0,0,0,0,0,0,0)
  override def persistenceId: String = "Cost" + "-" + context.parent.path.name + "-" + self.path.name
  // override def persistenceId : String = "x"

  override def onPersistFailure(cause: Throwable, event: Any, seqNr: Long): Unit = {
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage)) // TODO: used typed objects instead of raw strings (missing typed lang benefits), research: benefits from typing and monads, importance
    // fp simplified , de Alexander Kelvin
    log.error("persist failure being triggered")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def onPersistRejected(cause: Throwable, event: Any, seqNr: Long): Unit = {
    sender() ! OperationResponse("", Left("Failure"), Left(cause.getMessage))
    log.error("persist rejected being triggered")
    super.onPersistFailure(cause, event, seqNr)
  }

  override def receiveCommand: Receive = {
    case CreateTaxiTripCost(tripId,taxiTripCost) =>
      persist(TaxiTripCostCreatedEvent(tripId,taxiTripCost)) { _ =>
        log.info(s"Creating Taxi Cost $tripId at location ${self.path.name}")
        state = taxiTripCost
        log.info(s"Created cost stat: $taxiTripCost")
        // TODO: use specific log level         // May not succeed
        (costAggregator ? AddCostAggregatorValues(tripId,AggregatorStat(taxiTripCost.totalAmount,taxiTripCost.tripDistance,taxiTripCost.tipAmount))).pipeTo(sender())
        // sender() ! OperationResponse(tripId,Right("Success"))
      }

    case GetTaxiTripCost(tripId) =>
      log.info(s"Request to return Cost Info for tripId: $tripId")
      sender() ! state
    case UpdateTaxiTripCost(tripId,taxiTripCost,_) =>

      persist(UpdatedTaxiTripCostEvent(tripId, taxiTripCost)) { _ =>
        state = taxiTripCost
        (costAggregator ? UpdateCostAggregatorValues(
          tripId,
          taxiTripCost.totalAmount-state.totalAmount,
          taxiTripCost.tripDistance-state.tripDistance,
          taxiTripCost.tipAmount - state.tipAmount, // new minus old
          state.tipAmount)).pipeTo(sender())
        // sender() ! OperationResponse(tripId,Right("Success"))
        log.info(s"Updated cost stat: $taxiTripCost")
        }

    case DeleteTaxiTripCost(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripCostEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
        sender() ! OperationResponse(tripId,Right("Success"))
      }
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripCostCreatedEvent(tripId,taxiTripCost) =>
      log.info(s"Recovering Taxi Cost Created  $tripId ")
      state = taxiTripCost
      log.info(s"The state is $state from recovered $taxiTripCost")
      log.info(s"Location recovered $persistenceId")

    case UpdatedTaxiTripCostEvent(tripId,taxiTripCost) =>
      log.info(s"Recovering Taxi Cost Updated $tripId ")
      state = taxiTripCost

    case DeletedTaxiTripCostEvent(tripId) =>
      log.info(s"Recovering Taxi Cost Deleted for $tripId ")
      state = state.copy(deletedFlag = true)
  }
}


