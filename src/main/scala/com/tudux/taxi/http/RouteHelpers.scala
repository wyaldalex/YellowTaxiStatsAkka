package com.tudux.taxi.http

import com.tudux.taxi.actors.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.cost.TaxiTripCostCommand._
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfoCommand.UpdateTaxiTripExtraInfo
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTrip
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfoCommand.UpdateTaxiTripPassenger
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfoCommand.UpdateTaxiTripTimeInfo
import com.tudux.taxi.actors.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors._
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import spray.json._

object RouteHelpers {

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

  trait TaxiCostStatProtocol extends DefaultJsonProtocol {
    implicit val taxiCostStatFormat = jsonFormat11(TaxiTripCost)
  }

  trait TaxiTimeInfoStatProtocol extends DefaultJsonProtocol {
    implicit val taxiTimeInfoFormat = jsonFormat3(TaxiTripTimeInfo)
  }

  trait TaxiPassengerInfoProtocol extends DefaultJsonProtocol {
    implicit val taxiPassengerFormat = jsonFormat2(TaxiTripPassengerInfo)
  }

  trait TaxiExtraInfoProtocol extends DefaultJsonProtocol {
    implicit val taxiExtraInfoFormat = jsonFormat7(TaxiTripExtraInfo)
  }

  trait CalculateDistanceCostProtocol extends DefaultJsonProtocol {
    implicit val calculateDistanceCostFormat = jsonFormat1(CalculateTripDistanceCostResponse)
  }

  trait CalculateAverageTripTimeProtocol extends DefaultJsonProtocol {
    implicit val averageTripTimeFormat = jsonFormat1(TaxiTripAverageTimeMinutesResponse)
  }

  trait GetAverageTipAmountProtocol extends DefaultJsonProtocol {
    implicit val averageTipAmountFormat = jsonFormat1(GetAverageTipAmountResponse)
  }

  trait GetTotalLoadedResponseProtocol extends DefaultJsonProtocol {
    implicit val totalLoadedResponseFormat = jsonFormat4(LoadedStatsResponse)
  }

}
