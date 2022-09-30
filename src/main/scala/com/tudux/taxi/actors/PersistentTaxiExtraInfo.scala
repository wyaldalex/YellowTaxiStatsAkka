package com.tudux.taxi.actors

import akka.actor.{ActorLogging, Props}
import akka.persistence.PersistentActor

case class TaxiExtraInfoStat(pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                             store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double, deletedFlag: Boolean = false)

sealed trait TaxiExtraInfoCommand
object TaxiExtraInfoStatCommand {
  case class CreateTaxiExtraInfoStat(statId: String,taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoCommand
  case class GetTaxiExtraInfoStat(statId: String)
}


sealed trait TaxiExtraInfoEvent
object TaxiExtraInfoStatEvent{
  case class TaxiExtraInfoStatCreatedEvent(statId: String, taxiExtraInfoStat: TaxiExtraInfoStat) extends TaxiExtraInfoEvent
}

object PersistentTaxiExtraInfo {
  def props(id: String): Props = Props(new PersistentTaxiExtraInfo(id))
}
class PersistentTaxiExtraInfo(id: String) extends PersistentActor with ActorLogging {

  import TaxiExtraInfoStatCommand._
  import TaxiExtraInfoStatEvent._

  //Persistent Actor State
  var statCounter: Int = 1
  var statExtraInfoMap : Map[String,TaxiExtraInfoStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiExtraInfoStat(statId,taxiExtraInfoStat) =>
      persist(TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat)) { _ =>
        log.info(s"Creating Extra Info Stat $taxiExtraInfoStat")
        statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
        statCounter += 1
      }
    case GetTaxiExtraInfoStat(statId) =>
      sender() ! statExtraInfoMap.get(statId)
    case _ =>
      log.info(s"Received something else at ${self.path.name}")

  }

  override def receiveRecover: Receive = {
    case TaxiExtraInfoStatCreatedEvent(statId,taxiExtraInfoStat) =>
      log.info(s"Recovering Extra Info Stat $taxiExtraInfoStat")
      statExtraInfoMap = statExtraInfoMap + (statId -> taxiExtraInfoStat)
      statCounter += 1
  }
}


