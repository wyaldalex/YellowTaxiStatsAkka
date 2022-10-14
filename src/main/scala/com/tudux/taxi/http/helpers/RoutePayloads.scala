package com.tudux.taxi.http.helpers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import cats.data.Validated
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.TaxiTripEntry
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.UpdateTaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.UpdateTaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.UpdateTaxiTripPassenger
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.UpdateTaxiTripTimeInfo
import com.tudux.taxi.http.validation.Validation._
import com.tudux.taxi.http.validation.Validators._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object RoutePayloads {

  object CreateTaxiTripRequest {
    implicit val validator: Validator[CreateTaxiTripRequest] = createTaxiTripRequestValidator
  }
  case class CreateTaxiTripRequest(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                                   tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                                   storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                                   paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                                   tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double) {
    def toCommand: CreateTaxiTripCommand = CreateTaxiTripCommand(TaxiTripEntry(vendorID, tpepPickupDatetime, tpepDropoffDatetime, passengerCount,
      tripDistance, pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude,
      paymentType, fareAmount, extra, mtaTax,
      tipAmount, tollsAmount, improvementSurcharge, totalAmount))
  }

  case class UpdatePassengerInfoRequest(passengerCount: Int) {
    def toCommand(tripId: String): UpdateTaxiTripPassenger = UpdateTaxiTripPassenger(tripId, TaxiTripPassengerInfo(passengerCount))
  }

  case class UpdateExtraInfoRequest(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                                    storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double) {
    def toCommand(tripId: String): UpdateTaxiTripExtraInfo = UpdateTaxiTripExtraInfo(tripId, TaxiTripExtraInfo(pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude))
  }

  case class UpdateTimeInfoRequest(tpepPickupDatetime: String, tpepDropoffDatetime: String) {
    def toCommand(tripId: String): UpdateTaxiTripTimeInfo = UpdateTaxiTripTimeInfo(tripId, TaxiTripTimeInfo(tpepPickupDatetime, tpepDropoffDatetime))
  }

  object UpdateCostInfoRequest {
    implicit val validator: Validator[UpdateCostInfoRequest] = updateCostInfoRequestValidator
  }
  case class UpdateCostInfoRequest(vendorID: Int,
                                   tripDistance: Double,
                                   paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                                   tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double) {
    def toCommand(tripId: String): UpdateTaxiTripCost = UpdateTaxiTripCost(tripId, TaxiTripCost(vendorID,
      tripDistance,
      paymentType, fareAmount, extra, mtaTax,
      tipAmount, tollsAmount, improvementSurcharge, totalAmount))
  }

  case class LoadedStatsResponse(totalCostLoaded: Int, totalExtraInfoLoaded: Int, totalTimeInfoLoaded: Int, totalPassengerInfo: Int)
  case class FailureResponse(reason: String)

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route = {
    validateEntity(request) match {
      case Validated.Valid(_) => routeIfValid
      case Validated.Invalid(failures) =>
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }
  }


}
