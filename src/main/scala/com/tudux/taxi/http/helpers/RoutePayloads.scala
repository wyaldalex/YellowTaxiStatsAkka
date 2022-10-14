package com.tudux.taxi.http.helpers

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import cats.implicits._
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

object RoutePayloads {

  object CreateTaxiTripRequest {
    implicit val validator: Validator[CreateTaxiTripRequest] = (request: CreateTaxiTripRequest) => {
      val vendorIDValidation = validateRequired(request.vendorID,"vendorID")
      val tpepPickupDatetimeValidation = validateRequired(request.tpepPickupDatetime,"tpepPickupDatetime")
      val tpepDropoffDatetimeValidation = validateRequired(request.tpepDropoffDatetime,"tpepDropoffDatetime")
      val passengerCountValidation = validateRequired(request.passengerCount,"passengerCount")
      val tripDistanceValidation = validateRequired(request.tripDistance,"tripDistance")
      val pickupLongitudeValidation = validateRequired(request.pickupLongitude,"pickupLongitude")
      val pickupLatitudeValidation = validateRequired(request.pickupLatitude,"pickupLatitude")
      val rateCodeIDValidation = validateRequired(request.rateCodeID,"rateCodeID")
      val storeAndFwdFlagValidation = validateRequired(request.storeAndFwdFlag,"storeAndFwdFlag")
      val dropoffLongitudeValidation = validateRequired(request.dropoffLongitude,"dropoffLongitude")
      val dropoffLatitudeValidation = validateRequired(request.dropoffLatitude,"dropoffLatitude")
      val paymentTypeValidation = validateRequired(request.paymentType,"paymentType")
      val fareAmountValidation = validateRequired(request.fareAmount,"fareAmount")
      val extraValidation = validateRequired(request.extra,"extra")
      val mtaTaxValidation = validateRequired(request.mtaTax,"mtaTax")
      val tipAmountValidation = validateMinimum(request.tipAmount,0,"tipAmount")
      val tollsAmountValidation = validateRequired(request.tollsAmount,"tollsAmount")
      val improvementSurchargeValidation = validateRequired(request.improvementSurcharge,"improvementSurcharge")
      val totalAmountValidation = validateMinimum(request.totalAmount,0,"totalAmount")

      (vendorIDValidation,tpepPickupDatetimeValidation,tpepDropoffDatetimeValidation,passengerCountValidation,tripDistanceValidation,
        pickupLongitudeValidation,pickupLatitudeValidation,rateCodeIDValidation,storeAndFwdFlagValidation,dropoffLongitudeValidation,
        dropoffLatitudeValidation,paymentTypeValidation,fareAmountValidation,extraValidation,mtaTaxValidation,tipAmountValidation,
        tollsAmountValidation,improvementSurchargeValidation,totalAmountValidation).mapN(CreateTaxiTripRequest.apply)

    }
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

}
