package com.tudux.taxi.actors

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import kantan.csv.RowDecoder

import scala.concurrent.duration._
import java.io.File
import scala.concurrent.ExecutionContext
import scala.io.Source

case class TaxiCostStat(VendorID: Int,
                    trip_distance: Double,
                    payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                    tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double)


sealed trait TaxiCostCommand
object TaxiCostStatCommand {
  case class CreateTaxiCostStat(statId: Int,taxiCostStat: TaxiCostStat) extends TaxiCostCommand
}


sealed trait TaxiCostEvent
object TaxiCostStatEvent{
  case class TaxiCostStatCreatedEvent(statId: Int, taxiCostStat: TaxiCostStat) extends TaxiCostEvent
}

object PersistentTaxiTripCost {
  def props(id: String): Props = Props(new PersistentTaxiTripCost(id))
}
class PersistentTaxiTripCost(id: String) extends PersistentActor with ActorLogging {

  import TaxiCostStatCommand._
  import TaxiCostStatEvent._

  //Persistent Actor State
  var statCounter: Int = 1
  var statCostMap : Map[Int,TaxiCostStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiCostStat(statId,taxiCostStat) =>
      persist(TaxiCostStatCreatedEvent(statId,taxiCostStat)) { _ =>
        log.info("Creating Taxi Cost Stat")
        statCostMap = statCostMap + (statId -> taxiCostStat)
        statCounter += 1
      }
    case _ =>
      log.info("Received something else at PersistentTaxiTripCost")

  }

  override def receiveRecover: Receive = {
    case TaxiCostStatCreatedEvent(statId,taxiCostStat) =>
      log.info(s"Recovering Taxi Cost Stat $taxiCostStat")
      statCostMap = statCostMap + (statId -> taxiCostStat)
      statCounter += 1
  }
}


