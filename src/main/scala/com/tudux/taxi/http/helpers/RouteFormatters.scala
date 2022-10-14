package com.tudux.taxi.http.helpers

import spray.json.DefaultJsonProtocol
import RoutePayloads._
import com.tudux.taxi.actors.CostAggregatorResponse.{CalculateTripDistanceCostResponse, GetAverageTipAmountResponse}
import com.tudux.taxi.actors.TimeAggregatorResponse.TaxiTripAverageTimeMinutesResponse
import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo

object RouteFormatters {

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
