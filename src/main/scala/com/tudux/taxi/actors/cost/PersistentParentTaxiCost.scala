package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiStatResponseResponses, TaxiTripCommand, TaxiTripEvent}




object PersistentParentTaxiCost {

  def props(id: String) : Props = Props(new PersistentParentTaxiCost(id))
  case class TaxiTripCostState(costs: Map[String, ActorRef])
}
class PersistentParentTaxiCost(id: String) extends PersistentActor with ActorLogging {

  import PersistentParentTaxiCost._
  import TaxiCostStatCommand._
  import TaxiStatResponseResponses._
  import TaxiTripCommand._
  import TaxiTripEvent._

  var state: TaxiTripCostState = TaxiTripCostState(Map.empty)

  def createTaxiCostActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripCost.props(id), id)
  }
  
  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      val newTaxiTripCostActor = createTaxiCostActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(costs = state.costs + (statId -> newTaxiTripCostActor))
        newTaxiTripCostActor ! CreateTaxiCostStat(statId, taxiStat)
      }
      sender() ! TaxiStatCreatedResponse(statId)

    case GetTotalTaxiCostStats =>
      log.info("To be implemented")
      log.info(s"Received petition to return size which is: ${state.costs.size})")
    //Individual Gets
    case getTaxiCostStat@GetTaxiCostStat(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor.forward(getTaxiCostStat)
      
    //Individual Updates
    case updateTaxiCostStat@UpdateTaxiCostStat(statId, taxiCostStat, costAggregatorActor) =>
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor.forward(UpdateTaxiCostStat(statId, taxiCostStat, costAggregatorActor))
    //General Delete
    case deleteTaxiStat@DeleteTaxiStat(statId) =>
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor ! DeleteTaxiCostStat(statId)

    case GetTotalCostLoaded =>
      log.info("Returning total cost info loaded size")
      sender() ! state.costs.size

    //Individual Stats
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip Cost for id: $statId")
      val costActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiTripCost.props(statId), statId))
      state = state.copy(costs = state.costs + (statId -> costActor))
  }

}
