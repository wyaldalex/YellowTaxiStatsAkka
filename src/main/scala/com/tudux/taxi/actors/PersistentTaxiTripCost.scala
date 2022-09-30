package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.TaxiCostStatsResponse.CalculateTripDistanceCostResponse

case class TaxiCostStat(VendorID: Int,
                    trip_distance: Double,
                    payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                    tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double, deletedFlag: Boolean = false)


sealed trait TaxiCostCommand
object TaxiCostStatCommand {
  case class CreateTaxiCostStat(statId: String,taxiCostStat: TaxiCostStat) extends TaxiCostCommand
  case class GetTaxiCostStat(statId: String) extends  TaxiCostCommand
  case object GetTotalTaxiCostStats extends  TaxiCostCommand
  case class UpdateTaxiCostStat(statId: String,taxiCostStat: TaxiCostStat) extends TaxiCostCommand
  case class DeleteTaxiCostStat(statId: String) extends TaxiCostCommand
  case class CalculateTripDistanceCost(distance: Double)
}

sealed trait TaxiCostResponse
object TaxiCostStatsResponse {
  case class TotalTaxiCostStats(total: Int,totalAmount: Double)
  case class CalculateTripDistanceCostResponse(estimatedCost: Double)
}


sealed trait TaxiCostEvent
object TaxiCostStatEvent{
  case class TaxiCostStatCreatedEvent(statId: String, taxiCostStat: TaxiCostStat) extends TaxiCostEvent
  case class UpdatedTaxiCostStatEvent(statId: String,taxiCostStat: TaxiCostStat) extends TaxiCostEvent
  case class DeletedTaxiCostStatEvent(statId: String) extends TaxiCostEvent
}

object PersistentTaxiTripCost {
  def props(id: String): Props = Props(new PersistentTaxiTripCost(id))
}
class PersistentTaxiTripCost(id: String) extends PersistentActor with ActorLogging {

  import TaxiCostStatCommand._
  import TaxiCostStatEvent._

  //Persistent Actor State
  var statCostMap : Map[String,TaxiCostStat] = Map.empty
  //Used for domain specific utility
  var totalDistance : Double = 0
  var totalAmount : Double = 0

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiCostStat(statId,taxiCostStat) =>
      persist(TaxiCostStatCreatedEvent(statId,taxiCostStat)) { _ =>
        log.info("Creating Taxi Cost Stat")
        statCostMap = statCostMap + (statId -> taxiCostStat)
        totalAmount += taxiCostStat.total_amount
        totalDistance += taxiCostStat.trip_distance
      }
    case GetTotalTaxiCostStats =>
      log.info(s"Received petition to return size which is: ${statCostMap.size})")

    case GetTaxiCostStat(statId) =>
      sender() ! statCostMap.get(statId)
    case UpdateTaxiCostStat(statId,taxiCostStat) =>
      log.info("Updating taxi cost stat")
      if (statCostMap.contains(statId)) {
        persist(UpdatedTaxiCostStatEvent(statId, taxiCostStat)) { _ =>
          val prevAmount = statCostMap(statId).total_amount
          val prevDistance = statCostMap(statId).trip_distance
          statCostMap = statCostMap + (statId -> taxiCostStat)
          totalAmount += statCostMap(statId).total_amount - prevAmount
          totalDistance += statCostMap(statId).trip_distance - prevDistance
        }
      } else {
        log.info(s"Entry not found to update by id $statId")
      }
    case DeleteTaxiCostStat(statId) =>
      log.info("Deleting taxi cost stat")
      if(statCostMap.contains(statId)) {
        persist(DeletedTaxiCostStatEvent(statId)) { _ =>
          val taxiCostStatToBeDeleted: TaxiCostStat = statCostMap(statId).copy(deletedFlag = true)
          statCostMap = statCostMap + (statId -> taxiCostStatToBeDeleted)
        }
      }
    case CalculateTripDistanceCost(distance) =>
      log.info("Calculating estimated trip cost")
      sender() ! CalculateTripDistanceCostResponse((totalAmount/totalDistance) * distance)

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiCostStatCreatedEvent(statId,taxiCostStat) =>
      log.info(s"Recovering Taxi Cost Stat $taxiCostStat")
      statCostMap = statCostMap + (statId -> taxiCostStat)
      totalAmount += taxiCostStat.total_amount
      totalDistance += taxiCostStat.trip_distance
    case UpdatedTaxiCostStatEvent(statId,taxiCostStat) =>
      val prevAmount = statCostMap(statId).total_amount
      val prevDistance = statCostMap(statId).trip_distance
      statCostMap = statCostMap + (statId -> taxiCostStat)
      totalAmount += statCostMap(statId).total_amount - prevAmount
      totalDistance += statCostMap(statId).trip_distance - prevDistance
    case DeletedTaxiCostStatEvent(statId) =>
      val taxiCostStatToBeDeleted: TaxiCostStat = statCostMap(statId).copy(deletedFlag = true)
      statCostMap = statCostMap + (statId -> taxiCostStatToBeDeleted)
  }
}


