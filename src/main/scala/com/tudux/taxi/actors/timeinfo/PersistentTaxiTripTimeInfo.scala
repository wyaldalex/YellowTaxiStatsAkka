package com.tudux.taxi.actors.timeinfo

import akka.actor.{ActorLogging, ActorRef, Props}
import akka.persistence.PersistentActor
import com.tudux.taxi.actors.aggregators.TimeAggregatorCommand.UpdateTimeAggregatorValues

case class TaxiTripTimeInfo(tpepPickupDatetime: String, tpepDropoffDatetime: String, deletedFlag: Boolean = false)

sealed trait TaxiTripTimeInfoCommand
object TaxiTripTimeInfoCommand {
  case class CreateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoCommand
  case class GetTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
  case class UpdateTaxiTripTimeInfo(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo, timeAggregator: ActorRef = null) extends TaxiTripTimeInfoCommand
  case class DeleteTaxiTripTimeInfo(tripId: String) extends TaxiTripTimeInfoCommand
  case object GetTotalTimeInfoInfoLoaded
}


sealed trait TaxiTripTimeInfoEvent
object TaxiTripTimeInfoStatEvent{
  case class TaxiTripTimeInfoCreatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class TaxiTripTimeInfoUpdatedEvent(tripId: String, taxiTripTimeInfoStat: TaxiTripTimeInfo) extends TaxiTripTimeInfoEvent
  case class DeletedTaxiTripTimeInfoEvent(tripId: String) extends TaxiTripTimeInfoEvent
}


object PersistentTaxiTripTimeInfo {
  def props(id: String): Props = Props(new PersistentTaxiTripTimeInfo(id))
}
class PersistentTaxiTripTimeInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiTripTimeInfoCommand._
  import TaxiTripTimeInfoStatEvent._


  var state : TaxiTripTimeInfo = TaxiTripTimeInfo("","")

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiTripTimeInfo(tripId,taxiTripTimeInfoStat) =>
      persist(TaxiTripTimeInfoCreatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        log.info(s"Creating Trip Time Info Stat $taxiTripTimeInfoStat")
        state = taxiTripTimeInfoStat
      }
    case UpdateTaxiTripTimeInfo(tripId, taxiTripTimeInfoStat, timeAggregator) =>
      log.info("Updating Time Info ")
      persist(TaxiTripTimeInfoUpdatedEvent(tripId,taxiTripTimeInfoStat)) { _ =>
        timeAggregator ! UpdateTimeAggregatorValues(state,taxiTripTimeInfoStat)
        state = taxiTripTimeInfoStat
      }
    case GetTaxiTripTimeInfo(_) =>
      sender() ! state
    case DeleteTaxiTripTimeInfo(tripId) =>
      log.info("Deleting taxi cost stat")
      persist(DeletedTaxiTripTimeInfoEvent(tripId)) { _ =>
        state = state.copy(deletedFlag = true)
      }

    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiTripTimeInfoCreatedEvent(tripId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Trip Time Info Stat $tripId")
      state = taxiTripTimeInfoStat

    case TaxiTripTimeInfoUpdatedEvent(tripId,taxiTripTimeInfoStat) =>
      log.info(s"Recovering Update Trip Time Info Stat $tripId")
      state = taxiTripTimeInfoStat

    case DeletedTaxiTripTimeInfoEvent(tripId) =>
      log.info(s"Recovering Deleted Trip Time Info Stat $tripId")
      state = state.copy(deletedFlag = true)


  }
}


