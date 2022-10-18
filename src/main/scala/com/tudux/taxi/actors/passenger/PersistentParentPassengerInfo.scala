package com.tudux.taxi.actors.passenger

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.cost.TaxiTripCostCommand
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripResponse, TaxiTripCommand, TaxiTripEvent}

object PersistentParentPassengerInfo {
  def props(id: String): Props = Props(new PersistentParentPassengerInfo(id))

  case class TaxiTripPassengerInfoState(
                           passengerinfo: Map[String, ActorRef]
                          )
}
class PersistentParentPassengerInfo(id: String) extends PersistentActor with ActorLogging  {

  import PersistentParentPassengerInfo._
  import TaxiTripCostCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._
  import TaxiTripPassengerInfoCommand._

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

        newTaxiPassengerInfoActor ! CreateTaxiTripPassengerInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    case getTaxiPassengerInfoStat@GetTaxiTripPassengerInfo(statId) =>
      log.info(s"Receive Taxi Passenger Info Inquiry, forwarding")
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(getTaxiPassengerInfoStat)
    //Individual Updates
    case updateTaxiPassenger@UpdateTaxiTripPassenger(statId, _) =>
      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor.forward(updateTaxiPassenger)

    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>

      val taxiPassengerInfoActor = state.passengerinfo(statId)
      taxiPassengerInfoActor ! DeleteTaxiTripPassenger(statId)

    //Individual Stats
    case GetTotalPassengerInfoLoaded =>
      log.info("Returning total passenger info loaded size")
      sender() ! state.passengerinfo.size

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
