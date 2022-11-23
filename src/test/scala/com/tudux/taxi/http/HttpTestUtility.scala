package com.tudux.taxi.http

import spray.json.DefaultJsonProtocol

object HttpTestUtility {
  case class CreateTaxiTripRequest(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String,
    passengerCount: Int, tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double,
    rateCodeID: Int,
    storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
    paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
    tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double)

  trait CreateTaxiTripRequestProtocol extends DefaultJsonProtocol {
    implicit val CreateTaxiTripRequestFormat = jsonFormat19(CreateTaxiTripRequest)
  }
}
