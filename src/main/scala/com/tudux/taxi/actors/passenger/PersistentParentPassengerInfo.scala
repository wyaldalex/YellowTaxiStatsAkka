package com.tudux.taxi.actors.passenger

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.cost.TaxiCostStatCommand
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiStatResponseResponses, TaxiTripCommand, TaxiTripEvent}

object PersistentParentPassengerInfo {
  def props(id: String): Props = Props(new PersistentParentPassengerInfo(id))

  case class TaxiTripPassengerInfoState(
                           passengerinfo: Map[String, ActorRef]
                          )
}
class PersistentParentPassengerInfo(id: String) extends PersistentActor with ActorLogging  {

  import PersistentParentPassengerInfo._
  import TaxiCostStatCommand._
  import TaxiStatResponseResponses._
  import TaxiTripCommand._
  import TaxiTripEvent._
  import TaxiTripPassengerInfoStatCommand._

  var state: TaxiTripPassengerInfoState = TaxiTripPassengerInfoState(Map.empty)

  def createPassengerInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiTripPassengerInfo.props(id), id)
  }


  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiPassengerInfoActor = createPassengerInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(passengerinfo = state.passengerinfo + (statId -> newTaxiPassengerInfoActor))

        newTaxiPassengerInfoActor ! CreateTaxiTripPassengerInfoStat(statId, taxiStat)
      }
      sender() ! TaxiStatCreatedResponse(statId)

    case getTaxiPassengerInfoStat@GetTaxiPassengerInfoStat(statId) =>
      log.info(s"Receive Taxi Passenger Info Inquiry, forwarding")
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(getTaxiPassengerInfoStat)
    //Individual Updates
    case updateTaxiPassenger@UpdateTaxiPassenger(statId, _) =>
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(updateTaxiPassenger)

    //General Delete
    case deleteTaxiStat@DeleteTaxiStat(statId) =>

      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor ! DeleteTaxiTripPassenger(statId)

    //Individual Stats
    case getTotalPassengerInfoLoaded@GetTotalPassengerInfoLoaded =>
      log.info("To be implemented")
    //      taxiPassengerInfoActor.forward(getTotalPassengerInfoLoaded)

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
      val passengerActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiTripPassengerInfo.props(statId), statId))
      state = state.copy(passengerinfo = state.passengerinfo + (statId -> passengerActor))

  }

}
