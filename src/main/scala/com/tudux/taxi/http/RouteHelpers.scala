package com.tudux.taxi.http

import com.tudux.taxi.actors.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.TaxiCostStatCommand._
import com.tudux.taxi.actors.TaxiExtraInfoStatCommand.UpdateTaxiExtraInfoStat
import com.tudux.taxi.actors.TaxiStatCommand.CreateTaxiStat
import com.tudux.taxi.actors.TaxiTripCommand.CreateTaxiTripCommand
import com.tudux.taxi.actors.TaxiTripPassengerInfoStatCommand.UpdateTaxiPassenger
import com.tudux.taxi.actors.TaxiTripTimeInfoStatCommand.UpdateTaxiTripTimeInfoStat
import com.tudux.taxi.actors.TaxiTripTimeResponses.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors._
import spray.json._

object RouteHelpers {

  case class CreateTaxiStatRequest(vendorID: Int, tpepPickupDatetime: String, tpepDropoffDatetime: String, passengerCount: Int,
                                   tripDistance: Double, pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                                   storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double,
                                   paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                                   tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double) {
    def toCommand: CreateTaxiTripCommand = CreateTaxiTripCommand(TaxiStat(vendorID, tpepPickupDatetime, tpepDropoffDatetime, passengerCount,
      tripDistance, pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude,
      paymentType, fareAmount, extra, mtaTax,
      tipAmount, tollsAmount, improvementSurcharge, totalAmount))
  }

  case class UpdatePassengerInfoRequest(passengerCount: Int) {
    def toCommand(statId: String): UpdateTaxiPassenger = UpdateTaxiPassenger(statId, TaxiTripPassengerInfoStat(passengerCount))
  }

  case class UpdateExtraInfoRequest(pickupLongitude: Double, pickupLatitude: Double, rateCodeID: Int,
                                    storeAndFwdFlag: String, dropoffLongitude: Double, dropoffLatitude: Double) {
    def toCommand(statId: String): UpdateTaxiExtraInfoStat = UpdateTaxiExtraInfoStat(statId, TaxiExtraInfoStat(pickupLongitude, pickupLatitude, rateCodeID,
      storeAndFwdFlag, dropoffLongitude, dropoffLatitude))
  }

  case class UpdateTimeInfoRequest(tpepPickupDatetime: String, tpepDropoffDatetime: String) {
    def toCommand(statId: String): UpdateTaxiTripTimeInfoStat = UpdateTaxiTripTimeInfoStat(statId, TaxiTripTimeInfoStat(tpepPickupDatetime, tpepDropoffDatetime))
  }

  case class UpdateCostInfoRequest(vendorID: Int,
                                   tripDistance: Double,
                                   paymentType: Int, fareAmount: Double, extra: Double, mtaTax: Double,
                                   tipAmount: Double, tollsAmount: Double, improvementSurcharge: Double, totalAmount: Double) {
    def toCommand(statId: String): UpdateTaxiCostStat = UpdateTaxiCostStat(statId, TaxiCostStat(vendorID,
      tripDistance,
      paymentType, fareAmount, extra, mtaTax,
      tipAmount, tollsAmount, improvementSurcharge, totalAmount))
  }

  case class LoadedStatsResponse(totalCostLoaded: Int, totalExtraInfoLoaded: Int, totalTimeInfoLoaded: Int, totalPassengerInfo: Int)

  trait TaxiCostStatProtocol extends DefaultJsonProtocol {
    implicit val taxiCostStatFormat = jsonFormat11(TaxiCostStat)
  }

  trait TaxiTimeInfoStatProtocol extends DefaultJsonProtocol {
    implicit val taxiTimeInfoFormat = jsonFormat3(TaxiTripTimeInfoStat)
  }

  trait TaxiPassengerInfoProtocol extends DefaultJsonProtocol {
    implicit val taxiPassengerFormat = jsonFormat2(TaxiTripPassengerInfoStat)
  }

  trait TaxiExtraInfoProtocol extends DefaultJsonProtocol {
    implicit val taxiExtraInfoFormat = jsonFormat7(TaxiExtraInfoStat)
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
