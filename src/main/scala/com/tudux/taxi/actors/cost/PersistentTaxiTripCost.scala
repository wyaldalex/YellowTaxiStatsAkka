package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.CostAggregatorCommand.UpdateCostAggregatorValues

case class TaxiCostStat(vendorID: Int,
                    tripDistance: Double,
                    paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                    tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double, deletedFlag: Boolean = false)


sealed trait TaxiCostCommand
object TaxiCostStatCommand {
  case class CreateTaxiCostStat(statId: String,taxiCostStat: TaxiCostStat) extends TaxiCostCommand
  case class GetTaxiCostStat(statId: String) extends  TaxiCostCommand
  case object GetTotalTaxiCostStats extends  TaxiCostCommand
  case class UpdateTaxiCostStat(statId: String,taxiCostStat: TaxiCostStat, costAggregator: ActorRef = null) extends TaxiCostCommand
  case class DeleteTaxiCostStat(statId: String) extends TaxiCostCommand
  case object GetTotalCostLoaded extends TaxiCostCommand
  case class PrintTimeToLoad(startTimeMillis: Long) extends TaxiCostCommand
}

sealed trait TaxiCostResponse
object TaxiCostStatsResponse {
  case object TaxiCostCreated
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

  var state : TaxiCostStat = TaxiCostStat(0,0,0,0,0,0,0,0,0,0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiCostStat(statId,taxiCostStat) =>
      persist(TaxiCostStatCreatedEvent(statId,taxiCostStat)) { _ =>
        log.info(s"Creating Taxi Cost Stat $taxiCostStat")
        state = taxiCostStat
        log.info(s"Created cost stat: $taxiCostStat")
      }
      sender() ! s"${self.path} child actor registered"

    case GetTaxiCostStat(statId) =>
      log.info(s"Receiving request to return cost trip cost information ${self.path}")
      sender() ! state
    case UpdateTaxiCostStat(statId,taxiCostStat,costAggregator) =>

      persist(UpdatedTaxiCostStatEvent(statId, taxiCostStat)) { _ =>
        costAggregator ! UpdateCostAggregatorValues(
          taxiCostStat.totalAmount-state.totalAmount,
          taxiCostStat.tripDistance-state.tripDistance,
          taxiCostStat.totalAmount - state.tipAmount,
          state.tipAmount)
        state = taxiCostStat
        log.info(s"Updated cost stat: $taxiCostStat")
        }

    case DeleteTaxiCostStat(statId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiCostStatEvent(statId)) { _ =>
        state = state.copy(deletedFlag = true)
      }

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
      log.info(s"Recovering Taxi Cost Created  $statId at ${self.path}")
      state = taxiCostStat

    case UpdatedTaxiCostStatEvent(statId,taxiCostStat) =>
      log.info(s"Recovering Taxi Cost Updated $statId at ${self.path}")
      state = taxiCostStat

    case DeletedTaxiCostStatEvent(statId) =>
      state = state.copy(deletedFlag = true)
  }
}


