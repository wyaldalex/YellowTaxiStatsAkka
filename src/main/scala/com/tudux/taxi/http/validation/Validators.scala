package com.tudux.taxi.http.validation

import cats.implicits._
import com.tudux.taxi.http.payloads.RoutePayloads.{CreateTaxiTripRequest, UpdateCostInfoRequest, UpdateTimeInfoRequest}

object Validators {

  import Validation._

  def createTaxiTripRequestValidator : Validator[CreateTaxiTripRequest] = (request: CreateTaxiTripRequest) => {

    val vendorIDValidation = validateRequired(request.vendorID, "vendorID")
    val tpepPickupDatetimeValidation = validatePairDates(request.tpepPickupDatetime,request.tpepPickupDatetime,request.tpepDropoffDatetime,"tpepPickupDatetime","tpepDropoffDatetime")
    val tpepDropoffDatetimeValidation = validatePairDates(request.tpepPickupDatetime,request.tpepPickupDatetime,request.tpepDropoffDatetime,"tpepPickupDatetime","tpepDropoffDatetime")
    val passengerCountValidation = validateRequired(request.passengerCount, "passengerCount")
    val tripDistanceValidation = validateRequired(request.tripDistance, "tripDistance")
    val pickupLongitudeValidation = validateRequired(request.pickupLongitude, "pickupLongitude")
    val pickupLatitudeValidation = validateRequired(request.pickupLatitude, "pickupLatitude")
    val rateCodeIDValidation = validateRequired(request.rateCodeID, "rateCodeID")
    val storeAndFwdFlagValidation = validateRequired(request.storeAndFwdFlag, "storeAndFwdFlag")
    val dropoffLongitudeValidation = validateRequired(request.dropoffLongitude, "dropoffLongitude")
    val dropoffLatitudeValidation = validateRequired(request.dropoffLatitude, "dropoffLatitude")
    val paymentTypeValidation = validateRequired(request.paymentType, "paymentType")
    val fareAmountValidation = validateRequired(request.fareAmount, "fareAmount")
    val extraValidation = validateRequired(request.extra, "extra")
    val mtaTaxValidation = validateRequired(request.mtaTax, "mtaTax")
    val tipAmountValidation = validateMinimum(request.tipAmount, 0, "tipAmount")
    val tollsAmountValidation = validateRequired(request.tollsAmount, "tollsAmount")
    val improvementSurchargeValidation = validateRequired(request.improvementSurcharge, "improvementSurcharge")
    val totalAmountValidation = validateMinimum(request.totalAmount, 0, "totalAmount")

    (vendorIDValidation, tpepPickupDatetimeValidation, tpepDropoffDatetimeValidation, passengerCountValidation, tripDistanceValidation,
      pickupLongitudeValidation, pickupLatitudeValidation, rateCodeIDValidation, storeAndFwdFlagValidation, dropoffLongitudeValidation,
      dropoffLatitudeValidation, paymentTypeValidation, fareAmountValidation, extraValidation, mtaTaxValidation, tipAmountValidation,
      tollsAmountValidation, improvementSurchargeValidation, totalAmountValidation).mapN(CreateTaxiTripRequest.apply)

  }


  def updateCostInfoRequestValidator : Validator[UpdateCostInfoRequest] = (request: UpdateCostInfoRequest) => {
    val vendorIDValidation = validateRequired(request.vendorID, "vendorID")
    val tripDistanceValidation = validateRequired(request.tripDistance, "tripDistance")
    val paymentTypeValidation = validateRequired(request.paymentType, "paymentType")
    val fareAmountValidation = validateRequired(request.fareAmount, "fareAmount")
    val extraValidation = validateRequired(request.extra, "extra")
    val mtaTaxValidation = validateRequired(request.mtaTax, "mtaTax")
    val tipAmountValidation = validateMinimum(request.tipAmount, 0, "tipAmount")
    val tollsAmountValidation = validateRequired(request.tollsAmount, "tollsAmount")
    val improvementSurchargeValidation = validateRequired(request.improvementSurcharge, "improvementSurcharge")
    val totalAmountValidation = validateMinimum(request.totalAmount, 0, "totalAmount")

    (vendorIDValidation,tripDistanceValidation,paymentTypeValidation,fareAmountValidation,extraValidation,mtaTaxValidation,tipAmountValidation
    ,tollsAmountValidation,improvementSurchargeValidation,totalAmountValidation).mapN(UpdateCostInfoRequest.apply)
  }

  def updateTimeInfoRequestValidator : Validator[UpdateTimeInfoRequest] = (request: UpdateTimeInfoRequest) => {
    //value: A,date1: String, date2: String, fieldName: String, fieldName2 : String)
    val tpepPickupDatetimeValidation = validatePairDates(request.tpepPickupDatetime,request.tpepPickupDatetime,request.tpepDropoffDatetime,"tpepPickupDatetime","tpepDropoffDatetime")
    val tpepDropoffDatetimeValidation = validatePairDates(request.tpepPickupDatetime,request.tpepPickupDatetime,request.tpepDropoffDatetime,"tpepPickupDatetime","tpepDropoffDatetime")

    (tpepPickupDatetimeValidation,tpepDropoffDatetimeValidation).mapN(UpdateTimeInfoRequest.apply)

  }



}
