package com.tudux.taxi.actors.implicits

import com.tudux.taxi.actors.cost.TaxiTripCost
import com.tudux.taxi.actors.extrainfo.TaxiTripExtraInfo
import com.tudux.taxi.actors.passenger.TaxiTripPassengerInfo
import com.tudux.taxi.actors.timeinfo.TaxiTripTimeInfo
import com.tudux.taxi.actors.aggregators.AggregatorStat
import com.tudux.taxi.actors.aggregators.CostAggregatorCommand.AddCostAggregatorValues
import com.tudux.taxi.actors.loader.TaxiTripEntry

object TaxiTripImplicits {

  implicit def toTaxiCost(taxiStat: TaxiTripEntry): TaxiTripCost = {
    TaxiTripCost(taxiStat.vendorID, taxiStat.tripDistance,
      taxiStat.paymentType, taxiStat.fareAmount, taxiStat.extra, taxiStat.mtaTax,
      taxiStat.tipAmount, taxiStat.totalAmount, taxiStat.improvementSurcharge, taxiStat.totalAmount)
  }

  implicit def toTaxiExtraInfo(taxiStat: TaxiTripEntry): TaxiTripExtraInfo = {
    TaxiTripExtraInfo(taxiStat.pickupLongitude, taxiStat.pickupLatitude,
      taxiStat.rateCodeID, taxiStat.storeAndFwdFlag, taxiStat.dropoffLongitude, taxiStat.dropoffLatitude)
  }

  implicit def toTaxiPassengerInfo(taxiStat: TaxiTripEntry): TaxiTripPassengerInfo = {
    TaxiTripPassengerInfo(taxiStat.passengerCount)
  }

  implicit def toTaxiTimeInfoStat(taxiStat: TaxiTripEntry): TaxiTripTimeInfo = {
    TaxiTripTimeInfo(taxiStat.tpepPickupDatetime, taxiStat.tpepDropoffDatetime)
  }

  implicit def toAggregatorStat(taxiStat: TaxiTripEntry): AggregatorStat = {
    AggregatorStat(taxiStat.totalAmount, taxiStat.tripDistance, taxiStat.tipAmount)
  }

}
