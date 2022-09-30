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
  case class GetTaxiCostStat(statId: String)
  case object GetTotalTaxiCostStats
}

sealed trait TaxiCostResponse
object TaxiCostStatsResponse {
  case class TotalTaxiCostStats(total: Int,totalAmount: Double)
}


sealed trait TaxiCostEvent
object TaxiCostStatEvent{
  case class TaxiCostStatCreatedEvent(statId: String, taxiCostStat: TaxiCostStat) extends TaxiCostEvent
}

object PersistentTaxiTripCost {
  def props(id: String): Props = Props(new PersistentTaxiTripCost(id))
}
class PersistentTaxiTripCost(id: String) extends PersistentActor with ActorLogging {

  import TaxiCostStatCommand._
  import TaxiCostStatEvent._

  //Persistent Actor State
  var statCounter: Int = 1
  var statCostMap : Map[String,TaxiCostStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiCostStat(statId,taxiCostStat) =>
      persist(TaxiCostStatCreatedEvent(statId,taxiCostStat)) { _ =>
        log.info("Creating Taxi Cost Stat")
        statCostMap = statCostMap + (statId -> taxiCostStat)
        statCounter += 1
      }
    case GetTotalTaxiCostStats =>
      log.info(s"Received petition to return size which is: ${statCostMap.size})")

    case GetTaxiCostStat(statId) =>
      sender() ! statCostMap.get(statId)

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiCostStatCreatedEvent(statId,taxiCostStat) =>
      log.info(s"Recovering Taxi Cost Stat $taxiCostStat")
      statCostMap = statCostMap + (statId -> taxiCostStat)
      statCounter += 1
  }
}


