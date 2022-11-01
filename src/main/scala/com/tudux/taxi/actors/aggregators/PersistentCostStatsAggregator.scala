package com.tudux.taxi.actors.aggregators

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, SaveSnapshotFailure, SaveSnapshotSuccess, SnapshotOffer}
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse

//classes
case class AggregatorStat(totalAmount: Double, distance: Double, tipAmount: Double)

//commands
sealed trait CostAggregatorCommand
object  CostAggregatorCommand {
  case class AddCostAggregatorValues(tripId: String,stat: AggregatorStat)
  case class UpdateCostAggregatorValues(tripId: String,totalAmountDelta: Double, distanceDelta: Double, tipAmountDelta: Double, tipAmount: Double)
  case class CalculateTripDistanceCost(distance: Double) extends CostAggregatorCommand
  case object GetAverageTipAmount extends CostAggregatorCommand

}
//events
sealed trait CostAggregatorEvent
object CostAggregatorEvent{
  case class AddedCostAggregatorValuesEvent(tripId: String,stat: AggregatorStat) extends CostAggregatorEvent
  case class UpdatedCostAggregatorValuesEvent(totalAmountDelta: Double, distanceDelta: Double, tipAmountDelta: Double, tipAmount: Double) extends CostAggregatorEvent
}
//responses
sealed trait CostAggregatorResponse
object CostAggregatorResponse {
  case class CalculateTripDistanceCostResponse(estimatedCost: Double) extends CostAggregatorResponse
  case class GetAverageTipAmountResponse(averageTipAmount: Double) extends CostAggregatorResponse
}

object PersistentCostStatsAggregator {
  def props(id: String): Props = Props(new PersistentCostStatsAggregator(id))
}
class PersistentCostStatsAggregator(id: String) extends PersistentActor with ActorLogging {
  import CostAggregatorCommand._
  import CostAggregatorEvent._
  import CostAggregatorResponse._

  var totalDistance : Double = 0
  var totalAmount : Double = 0
  var numberOfTips : Int = 0
  var totalTipAmount : Double = 0
  var commandsWithoutCheckpoint = 0
  val maxMessages = 900

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case AddCostAggregatorValues(tripId,stat) =>
      log.info("Updating Cost Aggregator")
      persist(AddedCostAggregatorValuesEvent(tripId,stat)) { _ =>
        totalAmount += stat.totalAmount
        totalDistance += stat.distance
        if (stat.tipAmount > 0) {
          totalTipAmount += stat.tipAmount
          numberOfTips += 1
        }
        sender() ! OperationResponse(tripId,Right("Success"))
        maybeCheckpoint()
      }
    case CalculateTripDistanceCost(distance) =>
      log.info("Calculating estimated trip cost")
      sender() ! CalculateTripDistanceCostResponse((totalAmount/totalDistance) * distance)
    case GetAverageTipAmount =>
      sender() ! GetAverageTipAmountResponse(totalTipAmount / numberOfTips)
    //in case of distance or total amount updates special handling is requiered...
    case UpdateCostAggregatorValues(tripId,totalAmountDelta,distanceDelta,tipAmountDelta,tipAmount) =>
      log.info("Updating Cost Aggregator Values")
      persist(UpdatedCostAggregatorValuesEvent(totalAmountDelta, distanceDelta, tipAmountDelta, tipAmount)) { _ =>
        totalAmount += totalAmountDelta
        totalDistance += distanceDelta
        //new 140 old 100 n = +40
        //new 100 old 140 n = -40
        //new 100 old 0 = (+1)  (+tipAmount)
        //new 0 old n = (-1) (-tipAmountDelta)
        if (tipAmountDelta == tipAmount) { //new tip
          numberOfTips += 1
          totalTipAmount += tipAmount
        } else {
          totalTipAmount += tipAmountDelta
        }
        sender() ! OperationResponse(tripId,Right("Success"))
        maybeCheckpoint()
      }
    //SNAPSHOT related
    case SaveSnapshotSuccess(metadata) =>
      log.info(s"saving snapshot succeeded: $metadata")
    case SaveSnapshotFailure(metadata, reason) =>
      log.warning(s"saving snapshot $metadata failed because of $reason")
  }

  override def receiveRecover: Receive = {
    case AddedCostAggregatorValuesEvent(tripId,stat) =>
      log.info(s"Recovering cost aggregator stat $tripId")
      totalAmount += stat.totalAmount
      totalDistance += stat.distance
      if (stat.tipAmount > 0) {
        totalTipAmount += stat.tipAmount
        numberOfTips += 1
      }
    case UpdatedCostAggregatorValuesEvent(totalAmountDelta, distanceDelta, tipAmountDelta, tipAmount)  =>
      totalAmount += totalAmountDelta
      totalDistance += distanceDelta
      if (tipAmountDelta == tipAmount) { //new tip
        numberOfTips += 1
        totalTipAmount += tipAmount
      } else {
        totalTipAmount += tipAmountDelta
      }
    case SnapshotOffer(metadata, contents) =>
      //WARNING: Saved state has to match with the state update operations
      //saveSnapshot((totalDistance,totalAmount,numberOfTips,totalTipAmount))
      log.info(s"Recovered snapshot: $metadata")
      val snapState = contents.asInstanceOf[Tuple4[Double, Double,Int,Double]]
      totalDistance = snapState._1
      totalAmount = snapState._2
      numberOfTips = snapState._3
      totalTipAmount = snapState._4
  }

  def maybeCheckpoint(): Unit = {
    commandsWithoutCheckpoint += 1
    if (commandsWithoutCheckpoint >= maxMessages) {
      log.info("Saving checkpoint...")
      saveSnapshot((totalDistance,totalAmount,numberOfTips,totalTipAmount)) // save a tuple with the current actor state
      commandsWithoutCheckpoint = 0
    }
  }

}
