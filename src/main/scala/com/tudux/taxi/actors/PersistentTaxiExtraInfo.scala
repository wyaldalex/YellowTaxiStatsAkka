package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiExtraInfoStat(pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                             store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double, deletedFlag: Boolean = false)

sealed trait TaxiExtraInfoCommand
object TaxiExtraInfoStatCommand {
  case class CreateTaxiExtraInfoStat(statId: String,taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoCommand
  case class GetTaxiExtraInfoStat(statId: String) extends TaxiExtraInfoCommand
  case class UpdateTaxiExtraInfoStat(statId: String,taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoCommand
  case class DeleteTaxiExtraInfo(statId: String) extends TaxiExtraInfoCommand
  case object GetTotalExtraInfoLoaded
}


sealed trait TaxiExtraInfoEvent
object TaxiExtraInfoStatEvent{
  case class TaxiExtraInfoStatCreatedEvent(statId: String, taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoEvent
  case class TaxiExtraInfoStatUpdatedEvent(statId: String, taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoEvent
  case class DeletedTaxiExtraInfoEvent(statId: String) extends TaxiExtraInfoEvent
}

object PersistentTaxiExtraInfo {
  def props(id: String): Props = Props(new PersistentTaxiExtraInfo(id))
}
class PersistentTaxiExtraInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiExtraInfoStatCommand._
  import TaxiExtraInfoStatEvent._

  //Persistent Actor State
  var statExtraInfoMap : Map[String,TaxiExtraInfoStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiExtraInfoStat(statId,taxiExtraInfoStat) =>
      persist(TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat)) { _ =>
        log.info(s"Creating Extra Info Stat $taxiExtraInfoStat")
        statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
      }
    case UpdateTaxiExtraInfoStat(statId,taxiExtraInfoStat) =>
      log.info(s"Updating Extra Info Stat $taxiExtraInfoStat")
      if(statExtraInfoMap.contains(statId)) {
        persist(TaxiExtraInfoStatUpdatedEvent(statId, taxiExtraInfoStat)) { _ =>
          statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
        }
      } else log.info(s"Entry not found to update by id $statId")

    case GetTaxiExtraInfoStat(statId) =>
      sender() ! statExtraInfoMap.get(statId)
    case DeleteTaxiExtraInfo(statId) =>
      log.info("Deleting taxi cost stat")
      if (statExtraInfoMap.contains(statId)) {
        persist(DeletedTaxiExtraInfoEvent(statId)) { _ =>
          val taxiExtraInfoToBeDeleted: TaxiExtraInfoStat = statExtraInfoMap(statId).copy(deletedFlag = true)
          statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoToBeDeleted)
        }
      }
    case GetTotalExtraInfoLoaded =>
      sender() ! statExtraInfoMap.size
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat) =>
      log.info(s"Recovering Extra Info Stat $taxiExtraInfoStat")
      statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
    case TaxiExtraInfoStatUpdatedEvent(statId,taxiExtraInfoStat) =>
      log.info(s"Recovering Updated Extra Info Stat $taxiExtraInfoStat")
      statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
    case DeletedTaxiExtraInfoEvent(statId) =>
      log.info(s"Recovering Deleted Extra Info Stat")
      val taxiExtraInfoToBeDeleted: TaxiExtraInfoStat = statExtraInfoMap(statId).copy(deletedFlag = true)
      statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoToBeDeleted)

  }
}


