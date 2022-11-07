package com.tudux.taxi.http.payloads

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import cats.data.Validated
import com.tudux.taxi.actors.common.response.CommonOperationResponse.OperationResponse
import com.tudux.taxi.actors.loader.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.cost.TaxiTripCostCommand.{CreateTaxiTripCost, UpdateTaxiTripCost}
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.{CreateTaxiTripExtraInfo, UpdateTaxiTripExtraInfo}
import com.tudux.taxi.actors.loader.TaxiTripEntry
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.{CreateTaxiTripPassengerInfo, UpdateTaxiTripPassenger}
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.{CreateTaxiTripTimeInfo, UpdateTaxiTripTimeInfo}
import com.tudux.taxi.http.validation.Validation._
import com.tudux.taxi.http.validation.Validators._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

import scala.concurrent.Future

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

    def toCostCommand(tripId: String): CreateTaxiTripCost = CreateTaxiTripCost(tripId,TaxiTripCost(vendorID,
      tripDistance,
      paymentType, fareAmount, extra, mtaTax,
      tipAmount, tollsAmount, improvementSurcharge, totalAmount))

    def toExtraInfoCommand(tripId: String): CreateTaxiTripExtraInfo = CreateTaxiTripExtraInfo(tripId, TaxiTripExtraInfo(pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude))

    def toPassengerInfoCommand(tripId: String): CreateTaxiTripPassengerInfo = CreateTaxiTripPassengerInfo(tripId, TaxiTripPassengerInfo(passengerCount))

    def toTimeInfoCommand(tripId: String): CreateTaxiTripTimeInfo = CreateTaxiTripTimeInfo(tripId ,TaxiTripTimeInfo(tpepPickupDatetime, tpepDropoffDatetime))
  }

  object UpdatePassengerInfoRequest {
    implicit val validator: Validator[UpdatePassengerInfoRequest] = updatePassengerRequestValidator
  }
  case class UpdatePassengerInfoRequest(passengerCount: Int) {
    def toCommand(tripId: String): UpdateTaxiTripPassenger = UpdateTaxiTripPassenger(tripId, TaxiTripPassengerInfo(passengerCount))
  }

  object UpdateExtraInfoRequest {
    implicit val validator: Validator[UpdateExtraInfoRequest] = updateExtraInfoRequestValidator
  }
  case class UpdateExtraInfoRequest(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                                    storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double) {
    def toCommand(tripId: String): UpdateTaxiTripExtraInfo = UpdateTaxiTripExtraInfo(tripId, TaxiTripExtraInfo(pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude))
  }

  object UpdateTimeInfoRequest {
    implicit val validator: Validator[UpdateTimeInfoRequest] = updateTimeInfoRequestValidator
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
  case class CombinedTaxiTripOperationResponse(costResponse: OperationResponse, extraInfoResponse: OperationResponse,
                                               passengerResponse: OperationResponse, timeResponse: OperationResponse)
  case class FailureResponse(reason: String)

  def toHttpEntity(payload: String): HttpEntity.Strict = HttpEntity(ContentTypes.`application/json`, payload)

  def validateRequest[R: Validator](request: R)(routeIfValid: Route): Route = {
    validateEntity(request) match {
      case Validated.Valid(_) => routeIfValid
      case Validated.Invalid(failures) =>
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", ")))
    }
  }

  /*
  case class ValidatedRequestResponse(flag: Boolean, routeResult: Route)
  def validateRequestForDecision[R: Validator](request: R, validRoute: Route): ValidatedRequestResponse = {
    validateEntity(request) match {
      case Validated.Valid(_) => ValidatedRequestResponse(true,validRoute)
      case Validated.Invalid(failures) =>
        ValidatedRequestResponse(false,
        complete(StatusCodes.BadRequest, FailureResponse(failures.toList.map(_.errorMessage).mkString(", "))))
    }
  }
*/
  case class TaxiCreatedResponse(
                                  costResponse: Future[OperationResponse],
                                  extraInfoResponse: Future[OperationResponse],
                                  passengerResponse: Future[OperationResponse],
                                  timeResponse: Future[OperationResponse])


}
