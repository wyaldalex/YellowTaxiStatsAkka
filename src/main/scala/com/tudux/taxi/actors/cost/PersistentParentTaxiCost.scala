package com.tudux.taxi.actors.cost

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.cluster.sharding.ShardRegion
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripCommand, TaxiTripEvent, TaxiTripResponse}


object CostActorShardingSettings {

  import TaxiTripCommand._
  import TaxiTripCostCommand._

  val numberOfShards = 10 // use 10x number of nodes in your cluster
  val numberOfEntities = 100 //10x number of shards
  //this help to map the corresponding message to a respective entity
  val extractEntityId: ShardRegion.ExtractEntityId = {
    case createTaxiTripCommand@CreateTaxiTripCommand(taxiStat, statId) =>
      val entityId = statId.hashCode.abs % numberOfEntities
      (entityId.toString, createTaxiTripCommand)
    case msg@GetTaxiTripCost(statId) =>
      val shardId = statId.hashCode.abs % numberOfEntities
      (shardId.toString, msg)
  }

  //this help to map the corresponding message to a respective shard
  val extractShardId: ShardRegion.ExtractShardId = {
    case CreateTaxiTripCommand(taxiStat, statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case GetTaxiTripCost(statId) =>
      val shardId = statId.hashCode.abs % numberOfShards
      shardId.toString
    case ShardRegion.StartEntity(entityId) =>
      (entityId.toLong % numberOfShards).toString
  }

}

object PersistentParentTaxiCost {

  def props(id: String) : Props = Props(new PersistentParentTaxiCost(id))
  //a big state, candidate for sharding????
  case class TaxiTripCostState(costs: Map[String, ActorRef])
}
class PersistentParentTaxiCost(id: String) extends PersistentActor with ActorLogging {

  override def preStart(): Unit = {
    super.preStart()
    log.info("Sharded Parent Cost Started")
  }

  import PersistentParentTaxiCost._
  import TaxiTripCommand._
  import TaxiTripCostCommand._
  import TaxiTripEvent._
  import TaxiTripResponse._

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
