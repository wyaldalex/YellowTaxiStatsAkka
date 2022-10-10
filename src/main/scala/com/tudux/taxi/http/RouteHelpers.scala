package com.tudux.taxi.http

import com.tudux.taxi.actors.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.TaxiCostStatCommand._
import com.tudux.taxi.actors.TaxiExtraInfoStatCommand.UpdateTaxiExtraInfoStat
import com.tudux.taxi.actors.TaxiStatCommand.CreateTaxiStat
import com.tudux.taxi.actors.TaxiTripPassengerInfoStatCommand.UpdateTaxiPassenger
import com.tudux.taxi.actors.TaxiTripTimeInfoStatCommand.UpdateTaxiTripTimeInfoStat
import com.tudux.taxi.actors.TaxiTripTimeResponses.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors._
import spray.json._

object RouteHelpers {

  case class CreateTaxiStatRequest(VendorID: Int, tpep_pickup_datetime: String, tpep_dropoff_datetime: String, passenger_count: Int,
                                   trip_distance: Double, pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                                   store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double,
                                   payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                   tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {
    def toCommand: CreateTaxiStat = CreateTaxiStat(TaxiStat(VendorID, tpep_pickup_datetime, tpep_dropoff_datetime, passenger_count,
      trip_distance, pickup_longitude, pickup_latitude, RateCodeID,
      store_and_fwd_flag, dropoff_longitude, dropoff_latitude,
      payment_type, fare_amount, extra, mta_tax,
      tip_amount, tolls_amount, improvement_surcharge, total_amount))
  }

  case class UpdatePassengerInfoRequest(passenger_count: Int) {
    def toCommand(statId: String): UpdateTaxiPassenger = UpdateTaxiPassenger(statId, TaxiTripPassengerInfoStat(passenger_count))
  }

  case class UpdateExtraInfoRequest(pickup_longitude: Double, pickup_latitude: Double, RateCodeID: Int,
                                    store_and_fwd_flag: String, dropoff_longitude: Double, dropoff_latitude: Double) {
    def toCommand(statId: String): UpdateTaxiExtraInfoStat = UpdateTaxiExtraInfoStat(statId, TaxiExtraInfoStat(pickup_longitude, pickup_latitude, RateCodeID,
      store_and_fwd_flag, dropoff_longitude, dropoff_latitude))
  }

  case class UpdateTimeInfoRequest(tpep_pickup_datetime: String, tpep_dropoff_datetime: String) {
    def toCommand(statId: String): UpdateTaxiTripTimeInfoStat = UpdateTaxiTripTimeInfoStat(statId, TaxiTripTimeInfoStat(tpep_pickup_datetime, tpep_dropoff_datetime))
  }

  case class UpdateCostInfoRequest(VendorID: Int,
                                   trip_distance: Double,
                                   payment_type: Int, fare_amount: Double, extra: Double, mta_tax: Double,
                                   tip_amount: Double, tolls_amount: Double, improvement_surcharge: Double, total_amount: Double) {
    def toCommand(statId: String): UpdateTaxiCostStat = UpdateTaxiCostStat(statId, TaxiCostStat(VendorID,
      trip_distance,
      payment_type, fare_amount, extra, mta_tax,
      tip_amount, tolls_amount, improvement_surcharge, total_amount))
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
