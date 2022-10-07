package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

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
  case class CalculateTripDistanceCost(distance: Double) extends TaxiCostCommand
  case object GetAverageTipAmount extends TaxiCostCommand
  case object GetTotalCostLoaded extends TaxiCostCommand
  case class PrintTimeToLoad(startTimeMillis: Long) extends TaxiCostCommand
}

sealed trait TaxiCostResponse
object TaxiCostStatsResponse {
  case class TotalTaxiCostStats(total: Int,totalAmount: Double) extends TaxiCostResponse
  case class CalculateTripDistanceCostResponse(estimatedCost: Double) extends TaxiCostResponse
  case class GetAverageTipAmountResponse(averageTipAmount: Double)  extends TaxiCostResponse
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
  import TaxiCostStatsResponse._

  //Persistent Actor State
  //var statCostMap : Map[String,TaxiCostStat] = Map.empty
  var state : TaxiCostStat = TaxiCostStat(0,0,0,0,0,0,0,0,0,0)
  //Used for calculate estimated cost
  //var totalDistance : Double = 0
  //var totalAmount : Double = 0
  //var tipStats : Map[String,Double] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiCostStat(statId,taxiCostStat) =>
      persist(TaxiCostStatCreatedEvent(statId,taxiCostStat)) { _ =>
        log.info("Creating Taxi Cost Stat")
        state = taxiCostStat
        //statCostMap = statCostMap + (statId -> taxiCostStat)
//        totalAmount += taxiCostStat.total_amount
//        totalDistance += taxiCostStat.trip_distance
        //if(taxiCostStat.tip_amount > 0) tipStats =  tipStats + (statId -> taxiCostStat.tip_amount)
      }
    case GetTotalTaxiCostStats =>
      //log.info(s"Received petition to return size which is: ${statCostMap.size})")

    case GetTaxiCostStat(statId) =>
      log.info("Receiving request to return cost trip cost information")
      sender() ! state
    case UpdateTaxiCostStat(statId,taxiCostStat) =>
      /*
      log.info("Updating taxi cost stat")
      if (statCostMap.contains(statId)) {
        persist(UpdatedTaxiCostStatEvent(statId, taxiCostStat)) { _ =>
          val prevAmount = statCostMap(statId).total_amount
          val prevDistance = statCostMap(statId).trip_distance
          statCostMap = statCostMap + (statId -> taxiCostStat)
          totalAmount += statCostMap(statId).total_amount - prevAmount
          totalDistance += statCostMap(statId).trip_distance - prevDistance
          if(tipStats.contains(statId) && (taxiCostStat.tip_amount >0)) tipStats = (tipStats + (statId -> taxiCostStat.tip_amount))
          else if (tipStats.contains(statId) && (taxiCostStat.tip_amount == 0)) tipStats = (tipStats - statId)
        }
      } else {
        log.info(s"Entry not found to update by id $statId")
      } */
    case DeleteTaxiCostStat(statId) =>
      log.info("Deleting taxi cost stat")
      /*
      if(statCostMap.contains(statId)) {
        persist(DeletedTaxiCostStatEvent(statId)) { _ =>
          val taxiCostStatToBeDeleted: TaxiCostStat = statCostMap(statId).copy(deletedFlag = true)
          statCostMap = statCostMap + (statId -> taxiCostStatToBeDeleted)
        }
      }*/
    case CalculateTripDistanceCost(distance) =>
      log.info("Calculating estimated trip cost")
      //sender() ! CalculateTripDistanceCostResponse((totalAmount/totalDistance) * distance)
    case GetAverageTipAmount =>
      //sender() ! GetAverageTipAmountResponse(tipStats.values.sum / tipStats.size)

    case GetTotalCostLoaded =>
      //sender() ! statCostMap.size
    case PrintTimeToLoad(startTimeMillis) =>
      log.info("Getting Load Time")
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
      log.info(s"Total Load Time: $durationSeconds" )
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiCostStatCreatedEvent(statId,taxiCostStat) =>
      log.info(s"Recovering Taxi Cost Stat $taxiCostStat")
      //statCostMap = statCostMap + (statId -> taxiCostStat)
//      totalAmount += taxiCostStat.total_amount
//      totalDistance += taxiCostStat.trip_distance
      //if(taxiCostStat.tip_amount > 0) tipStats = tipStats + (statId -> taxiCostStat.tip_amount)

    case UpdatedTaxiCostStatEvent(statId,taxiCostStat) =>
//      val prevAmount = statCostMap(statId).total_amount
//      val prevDistance = statCostMap(statId).trip_distance
//      statCostMap = statCostMap + (statId -> taxiCostStat)
//      totalAmount += statCostMap(statId).total_amount - prevAmount
//      totalDistance += statCostMap(statId).trip_distance - prevDistance
//      if (tipStats.contains(statId) && (taxiCostStat.tip_amount > 0)) tipStats = (tipStats + (statId -> taxiCostStat.tip_amount))
//      else if (tipStats.contains(statId) && (taxiCostStat.tip_amount == 0)) tipStats = (tipStats - statId)
    case DeletedTaxiCostStatEvent(statId) =>
//      val taxiCostStatToBeDeleted: TaxiCostStat = statCostMap(statId).copy(deletedFlag = true)
//      statCostMap = statCostMap + (statId -> taxiCostStatToBeDeleted)
  }
}


