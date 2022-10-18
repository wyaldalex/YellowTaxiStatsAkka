package com.tudux.taxi.actors.extrainfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.helpers.TaxiTripHelpers._
import com.tudux.taxi.actors.{TaxiTripResponse, TaxiTripCommand, TaxiTripEvent}


object PersistentParentExtraInfo {
  def props(id: String) : Props = Props(new PersistentParentExtraInfo(id))
  case class TaxiTripExtraInfoState(extrainfo: Map[String, ActorRef])
}
class PersistentParentExtraInfo(id: String) extends PersistentActor with ActorLogging  {

  import PersistentParentExtraInfo._
  import TaxiTripExtraInfoCommand._
  import TaxiTripResponse._
  import TaxiTripCommand._
  import TaxiTripEvent._

  var state: TaxiTripExtraInfoState = TaxiTripExtraInfoState(Map.empty)

  def createTaxiExtraInfoActor(id: String): ActorRef = {
    context.actorOf(PersistentTaxiExtraInfo.props(id), id)
  }
  

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripCommand(taxiStat,statId) =>
      //generate new stat ID to avoid conflicts
      log.info(s"Received $taxiStat to create")
      //taxiTripCostActor.forward(CreateTaxiCostStat(idStat,taxiStat))
      val newTaxiExtraInfoActor = createTaxiExtraInfoActor(statId)
      persist(CreatedTaxiTripEvent(statId)) { event =>
        state = state.copy(extrainfo = state.extrainfo + ((statId) -> newTaxiExtraInfoActor))

        newTaxiExtraInfoActor ! CreateTaxiTripExtraInfo(statId, taxiStat)
      }
      sender() ! TaxiTripCreatedResponse(statId)

    case getTaxiExtraInfoStat@GetTaxiTripExtraInfo(statId) =>
      log.info(s"Receive Taxi Cost Inquiry, forwarding")
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor.forward(getTaxiExtraInfoStat)

    //Individual Updates

    case updateTaxiExtraInfoStat@UpdateTaxiTripExtraInfo(statId, _) =>
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor.forward(updateTaxiExtraInfoStat)

    //General Delete
    case deleteTaxiStat@DeleteTaxiTrip(statId) =>
      val taxiExtraInfoActor = state.extrainfo(statId)
      taxiExtraInfoActor ! DeleteTaxiTripExtraInfo(statId)

    //Individual Stats
    case GetTotalExtraInfoLoaded =>
      log.info("Returning total cost info loaded size")
      sender() ! state.extrainfo.size

    case message: String =>
      log.info(message)
    case _ => log.info("Received something else at parent actor")

  }
  override def receiveRecover: Receive = {
    case CreatedTaxiTripEvent(statId) =>
      log.info(s"Recovering Taxi Trip for id: $statId")
      val extraInfoActor = context.child(statId)
        .getOrElse(context.actorOf(PersistentTaxiExtraInfo.props(statId), statId))
      state = state.copy(extrainfo = state.extrainfo + (statId -> extraInfoActor))

  }

}
