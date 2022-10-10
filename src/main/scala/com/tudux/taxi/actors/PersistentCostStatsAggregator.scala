package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

import akka.persistence.PersistentActor

//classes
case class AggregatorStat(totalAmount: Double, distance: Double, tipAmount: Double)

//commands
sealed trait CostAggregatorCommand
object  CostAggregatorCommand {
  case class AddCostAggregatorValues(statId: String,stat: AggregatorStat)
  case class CalculateTripDistanceCost(distance: Double) extends CostAggregatorCommand
  case object GetAverageTipAmount extends CostAggregatorCommand

}
//events
sealed trait CostAggregatorEvent
object CostAggregatorEvent{
  case class AddedCostAggregatorValuesEvent(statId: String,stat: AggregatorStat)
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
  var tipStats : Map[String,Double] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case AddCostAggregatorValues(statId,stat) =>
      log.info("Updating Cost Aggregator")
      persist(AddedCostAggregatorValuesEvent(statId,stat)) { _ =>
        totalAmount += stat.totalAmount
        totalDistance += stat.distance
        if(stat.tipAmount > 0) tipStats =  tipStats + (statId -> stat.totalAmount)
      }
    case CalculateTripDistanceCost(distance) =>
      log.info("Calculating estimated trip cost")
      sender() ! CalculateTripDistanceCostResponse((totalAmount/totalDistance) * distance)
    case GetAverageTipAmount =>
      sender() ! GetAverageTipAmountResponse(tipStats.values.sum / tipStats.size)

  }

  override def receiveRecover: Receive = {
    case AddedCostAggregatorValuesEvent(statId,stat) =>
      log.info(s"Recovering cost aggregator stat $statId")
      totalAmount += stat.totalAmount
      totalDistance += stat.distance
      if (stat.tipAmount > 0) tipStats = tipStats + (statId -> stat.totalAmount)
  }

}
