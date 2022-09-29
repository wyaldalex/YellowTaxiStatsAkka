package com.tudux.taxi.http

import akka.pattern.ask
import akka.http.scaladsl.server.Directives._
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.tudux.taxi.actors.TaxiStat
import com.tudux.taxi.actors.TaxiStatCommand.CreateTaxiStat

import scala.concurrent.ExecutionContext
//import como.tudux.bank.actors.PersistentBankAccount.{Command, Response}
//import como.tudux.bank.actors.PersistentBankAccount.Command._
//import como.tudux.bank.actors.PersistentBankAccount.Response.{BankAccountBalanceUpdatedResponse, BankAccountCreatedResponse, GetBankAccountResponse}
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class CreateTaxiStatRequest(vendorID: Int, tpep_pickup_datetime: String, tpep_dropoff_datetime: String, passenger_count: Int,
                                 trip_distance: Double, pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                                 store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double,
                                 payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                 tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {
  def toCommand: CreateTaxiStat = CreateTaxiStat(TaxiStat(vendorID, tpep_pickup_datetime, tpep_dropoff_datetime, passenger_count,
    trip_distance, pickup_longitude, pickup_latitude, RateCodeID,
    store_and_fwd_flag, dropoff_longitude, dropoff_latitude,
    payment_type, fare_amount, extra, mta_tax,
    tip_amount, tolls_amount, improvement_surcharge, total_amount))
}

class TaxiStatsRouter(taxiTripActor: ActorRef)(implicit system: ActorSystem) {
  implicit val dispatcher: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = Timeout(5.seconds)

  val routes: Route =
    pathPrefix("api" / "yellowtaxi") {
      get {
        path("cost") {
          complete(StatusCodes.OK)
        }
      }
    }
}
