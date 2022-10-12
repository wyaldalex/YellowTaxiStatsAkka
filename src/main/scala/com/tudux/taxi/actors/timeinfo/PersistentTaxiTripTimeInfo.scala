package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.TimeAggregatorCommand.UpdateTimeAggregatorValues

case class TaxiTripTimeInfoStat(tpepPickupDatetime: String,tpepDropoffDatetime: String,deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoStatCommand {
  case class CreateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoCommand
  case class GetTaxiTimeInfoStat(statId: String) extends TaxiTripTimeInfoCommand
  case class UpdateTaxiTripTimeInfoStat(statId: String,taxiTripTimeInfoStat: TaxiTripTimeInfoStat, timeAggregator: ActorRef = null) extends TaxiTripTimeInfoCommand
  case class DeleteTaxiTripTimeInfoStat(statId: String) extends TaxiTripTimeInfoCommand
  case object GetTotalTimeInfoInfoLoaded
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoStatCreatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoStatUpdatedEvent(statId: String, taxiTripTimeInfoStat: TaxiTripTimeInfoStat) extends TaxiTripTimeInfoEvent
  case class DeletedTaxiTripTimeInfoStatEvent(statId: String) extends TaxiTripTimeInfoEvent
}



object TaxiTripTimeResponses {

}

object PersistentTaxiTripTimeInfo {
  def props(id: String): Props = Props(new PersistentTaxiTripTimeInfo(id))
}
class PersistentTaxiTripTimeInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripTimeInfoStatCommand._
  import TaxiTripTimeInfoStatEvent._


  var state : TaxiTripTimeInfoStat = TaxiTripTimeInfoStat("","")

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripTimeInfoStat(statId,taxiTripTimeInfoStat) =>
      persist(TaxiTripTimeInfoStatCreatedEvent(statId,taxiTripTimeInfoStat)) { _ =>
        log.info(s"Creating Trip Time Info Stat $taxiTripTimeInfoStat")
        state = taxiTripTimeInfoStat
      }
    case UpdateTaxiTripTimeInfoStat(statId, taxiTripTimeInfoStat, timeAggregator) =>
      log.info("Updating Time Info ")
      persist(TaxiTripTimeInfoStatUpdatedEvent(statId,taxiTripTimeInfoStat)) { _ =>
        timeAggregator ! UpdateTimeAggregatorValues(state,taxiTripTimeInfoStat)
        state = taxiTripTimeInfoStat
      }
    case GetTaxiTimeInfoStat(_) =>
      sender() ! state
    case DeleteTaxiTripTimeInfoStat(statId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripTimeInfoStatEvent(statId)) { _ =>
        state = state.copy(deletedFlag = true)
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripTimeInfoStatCreatedEvent(statId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Trip Time Info Stat $taxiTripTimeInfoStat")
      state = taxiTripTimeInfoStat

    case TaxiTripTimeInfoStatUpdatedEvent(statId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Update Trip Time Info Stat $taxiTripTimeInfoStat")
      state = taxiTripTimeInfoStat

    case DeletedTaxiTripTimeInfoStatEvent(statId) =>
      log.info(s"Recovering Deleted Trip Time Info Stat ")
      state = state.copy(deletedFlag = true)


  }
}


