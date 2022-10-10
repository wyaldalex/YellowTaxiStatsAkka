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
/*
2,2015-01-15 19:05:39,2015-01-15 19:23:42,1,1.59,-73.993896484375,40.750110626220703,1,N,-73.974784851074219,40.750617980957031,1,12,1,0.5,3.25,0,0.3,17.05
1,2015-01-10 20:33:38,2015-01-10 20:53:28,1,3.30,-74.00164794921875,40.7242431640625,1,N,-73.994415283203125,40.759109497070313,1,14.5,0.5,0.5,2,0,0.3,17.8
1,2015-01-10 20:33:38,2015-01-10 20:43:41,1,1.80,-73.963340759277344,40.802787780761719,1,N,-73.951820373535156,40.824413299560547,2,9.5,0.5,0.5,0,0,0.3,10.8
1,2015-01-10 20:33:39,2015-01-10 20:35:31,1,.50,-74.009086608886719,40.713817596435547,1,N,-74.004325866699219,40.719985961914063,2,3.5,0.5,0.5,0,0,0.3,4.8
1,2015-01-10 20:33:39,2015-01-10 20:52:58,1,3.00,-73.971176147460938,40.762428283691406,1,N,-74.004180908203125,40.742652893066406,2,15,0.5,0.5,0,0,0.3,16.3
1,2015-01-10 20:33:39,2015-01-10 20:53:52,1,9.00,-73.874374389648438,40.7740478515625,1,N,-73.986976623535156,40.758193969726563,1,27,0.5,0.5,6.7,5.33,0.3,40.33
1,2015-01-10 20:33:39,2015-01-10 20:58:31,1,2.20,-73.9832763671875,40.726009368896484,1,N,-73.992469787597656,40.7496337890625,2,14,0.5,0.5,0,0,0.3,15.3
 */

case class TaxiStat(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                          tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                          storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                          paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                          tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)


sealed trait Command
object TaxiStatCommand {
  case class CreateTaxiStat(taxiStat: TaxiStat) extends Command
  case class DeleteTaxiStat(statId: String) extends Command
}

sealed trait TaxiStatResponse
object TaxiStatResponseResponses {
  case class TaxiStatCreatedResponse(statId: String) extends TaxiStatResponse
}

sealed trait Event
object TaxiStatEvent{
  case class TaxiStatCreatedEvent(taxiStat: TaxiStat) extends Event
}
/*
object PersistentTaxiStatActor {
  def props(id: String): Props = Props(new PersistentTaxiStatActor(id))
}
class PersistentTaxiStatActor(id: String) extends PersistentActor with ActorLogging {
  import TaxiStatCommand._
  import TaxiStatEvent._

  //Persistent Actor State
  var statCounter: Int = 1
  var statMap : Map[Int,TaxiStat] = Map.empty

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case CreateTaxiStat(taxiStat) =>
      persist(TaxiStatCreatedEvent(taxiStat)) { _ =>
        log.info("Creating Stat")
        statMap = statMap + (statCounter -> taxiStat)
        statCounter += 1
      }
    case _ =>
      log.info("Received something else")

  }

  override def receiveRecover: Receive = {
    case TaxiStatCreatedEvent(taxiStat) =>
      log.info(s"Recovering Stat $taxiStat")
      statMap = statMap + (statCounter -> taxiStat)
      statCounter += 1
  }
}
*/

