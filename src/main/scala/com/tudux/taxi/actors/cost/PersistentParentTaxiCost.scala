package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripResponse, TaxiTripCommand, TaxiTripEvent}

object PersistentParentTaxiCost {

  def props(id: String) : Props = Props(new PersistentParentTaxiCost(id))
  //a big state, candidate for sharding????
  case class TaxiTripCostState(costs: Map[String, ActorRef])
}
class PersistentParentTaxiCost(id: String) extends PersistentActor with ActorLogging {
  import akka.cluster.sharding.ShardRegion.Passivate

  override def preStart(): Unit = {
    super.preStart()
    log.info("Sharded Parent Cost Started")
  }

  import PersistentParentTaxiCost._
  import TaxiTripCostCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._

  var state: TaxiTripCostState = TaxiTripCostState(Map.empty)

  def createTaxiCostActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripCost.props(id), id)
  }
  
  //override def persistenceId: String = id
  //override def persistenceId: String = "ShardedParentCostActor-" + self.path.name
  override def persistenceId: String = context.parent.path.name + "-" + self.path.name;

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      val newTaxiTripCostActor = createTaxiCostActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(costs = state.costs + (statId -> newTaxiTripCostActor))
        newTaxiTripCostActor ! CreateTaxiTripCost(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)
    //Individual Gets
    case getTaxiCostStat@GetTaxiTripCost(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding at ${self.path}")
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor.forward(getTaxiCostStat)
      
    //Individual Updates
    case updateTaxiCostStat@UpdateTaxiTripCost(statId, taxiCostStat, costAggregatorActor) =>
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor.forward(UpdateTaxiTripCost(statId, taxiCostStat, costAggregatorActor))
    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>
      val taxiTripCostActor = state.costs(statId)
      taxiTripCostActor ! DeleteTaxiTripCost(statId)

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
