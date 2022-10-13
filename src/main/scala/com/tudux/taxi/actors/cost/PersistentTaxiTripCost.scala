package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.CostAggregatorCommand.UpdateCostAggregatorValues

case class TaxiTripCost(vendorID: Int,
                        tripDistance: Double,
                        paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                        tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double, deletedFlag: Boolean = false)


sealed trait TaxiCostCommand
object TaxiTripCostCommand {
  case class CreateTaxiTripCost(statId: String, taxiTripCost: TaxiTripCost) extends TaxiCostCommand
  case class GetTaxiTripCost(statId: String) extends  TaxiCostCommand
  case object GetTotalTaxiTripCost extends  TaxiCostCommand
  case class UpdateTaxiTripCost(statId: String, taxiTripCost: TaxiTripCost, costAggregator: ActorRef = null) extends TaxiCostCommand
  case class DeleteTaxiTripCost(statId: String) extends TaxiCostCommand
  case object GetTotalCostLoaded extends TaxiCostCommand
  case class PrintTimeToLoad(startTimeMillis: Long) extends TaxiCostCommand
}


sealed trait TaxiCostEvent
object TaxiTripCostEvent{
  case class TaxiTripCostCreatedEvent(statId: String, taxiTripCost: TaxiTripCost) extends TaxiCostEvent
  case class UpdatedTaxiTripCostEvent(statId: String, taxiTripCost: TaxiTripCost) extends TaxiCostEvent
  case class DeletedTaxiTripCostEvent(statId: String) extends TaxiCostEvent
}

object PersistentTaxiTripCost {
  def props(id: String): Props = Props(new PersistentTaxiTripCost(id))
}
class PersistentTaxiTripCost(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripCostCommand._
  import TaxiTripCostEvent._

  var state : TaxiTripCost = TaxiTripCost(0,0,0,0,0,0,0,0,0,0)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripCost(statId,taxiTripCost) =>
      persist(TaxiTripCostCreatedEvent(statId,taxiTripCost)) { _ =>
        log.info(s"Creating Taxi Cost $taxiTripCost")
        state = taxiTripCost
        log.info(s"Created cost stat: $taxiTripCost")
      }
      sender() ! s"${self.path} child actor registered"

    case GetTaxiTripCost(statId) =>
      log.info(s"Receiving request to return cost trip cost information ${self.path}")
      sender() ! state
    case UpdateTaxiTripCost(statId,taxiTripCost,costAggregator) =>

      persist(UpdatedTaxiTripCostEvent(statId, taxiTripCost)) { _ =>
        costAggregator ! UpdateCostAggregatorValues(
          taxiTripCost.totalAmount-state.totalAmount,
          taxiTripCost.tripDistance-state.tripDistance,
          taxiTripCost.totalAmount - state.tipAmount,
          state.tipAmount)
        state = taxiTripCost
        log.info(s"Updated cost stat: $taxiTripCost")
        }

    case DeleteTaxiTripCost(statId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripCostEvent(statId)) { _ =>
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
    case TaxiTripCostCreatedEvent(statId,taxiTripCost) =>
      log.info(s"Recovering Taxi Cost Created  $statId ")
      state = taxiTripCost

    case UpdatedTaxiTripCostEvent(statId,taxiTripCost) =>
      log.info(s"Recovering Taxi Cost Updated $statId ")
      state = taxiTripCost

    case DeletedTaxiTripCostEvent(statId) =>
      log.info(s"Recovering Taxi Cost Deleted for $statId ")
      state = state.copy(deletedFlag = true)
  }
}


