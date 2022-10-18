package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripResponse, TaxiTripCommand, TaxiTripEvent}

object PersistentParentTimeInfo {
  def props(id: String) : Props = Props(new PersistentParentTimeInfo(id))

  case class TaxiTripTimeInfoState(
                           timeinfo: Map[String, ActorRef]
                          )
}
class PersistentParentTimeInfo(id: String) extends PersistentActor with ActorLogging {

  import PersistentParentTimeInfo._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._
  import TaxiTripTimeInfoCommand._
  import com.tudux.taxi.actors.cost.TaxiTripCostCommand._

  var state: TaxiTripTimeInfoState = TaxiTripTimeInfoState(Map.empty)
  
  def createTimeInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripTimeInfo.props(id), id)
  }

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiTimeInfoActor = createTimeInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(timeinfo = state.timeinfo + (statId -> newTaxiTimeInfoActor))

        newTaxiTimeInfoActor ! CreateTaxiTripTimeInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    //Individual Gets
    case getTaxiTimeInfoStat@GetTaxiTripTimeInfo(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor.forward(getTaxiTimeInfoStat)
    //Individual Updates
    case updateTaxiTripTimeInfoStat@UpdateTaxiTripTimeInfo(statId, taxiTripTimeInfoStat, timeAggregatorActor) =>
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor.forward(UpdateTaxiTripTimeInfo(statId, taxiTripTimeInfoStat, timeAggregatorActor))
    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>
      val taxiTimeInfoActor = state.timeinfo(statId)
      taxiTimeInfoActor ! DeleteTaxiTripTimeInfo(statId)
    //Individual Stats
    case GetTotalTimeInfoInfoLoaded =>
        log.info("Returning total time info loaded size")
        sender() ! state.timeinfo.size
    case printTimeToLoad@PrintTimeToLoad(_) =>
      log.info("Forwarding Total Time to Load Request")
    //      taxiTripCostActor.forward(printTimeToLoad)
    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }

  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip for id: $statId")
      val timeInfoActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiTripTimeInfo.props(statId), statId))
      state = state.copy(timeinfo = state.timeinfo + (statId -> timeInfoActor))
      
  }

}
