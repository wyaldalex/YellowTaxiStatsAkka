package com.tudux.taxi.actors.helpers

import com.tudux.taxi.actors.{AggregatorStat, TaxiCostStat, TaxiExtraInfoStat, TaxiStat, TaxiTripPassengerInfoStat, TaxiTripTimeInfoStat}

object TaxiTripHelpers {

  implicit def toTaxiCost(taxiStat: TaxiStat): TaxiCostStat = {
    TaxiCostStat(taxiStat.vendorID, taxiStat.tripDistance,
      taxiStat.paymentType, taxiStat.fareAmount, taxiStat.extra, taxiStat.mtaTax,
      taxiStat.tipAmount, taxiStat.totalAmount, taxiStat.improvementSurcharge, taxiStat.totalAmount)
  }

  implicit def toTaxiExtraInfo(taxiStat: TaxiStat): TaxiExtraInfoStat = {
    TaxiExtraInfoStat(taxiStat.pickupLongitude, taxiStat.pickupLatitude,
      taxiStat.rateCodeID, taxiStat.storeAndFwdFlag, taxiStat.dropoffLongitude, taxiStat.dropoffLatitude)
  }

  implicit def toTaxiPassengerInfo(taxiStat: TaxiStat): TaxiTripPassengerInfoStat = {
    TaxiTripPassengerInfoStat(taxiStat.passengerCount)
  }

  implicit def toTaxiTimeInfoStat(taxiStat: TaxiStat): TaxiTripTimeInfoStat = {
    TaxiTripTimeInfoStat(taxiStat.tpepPickupDatetime, taxiStat.tpepDropoffDatetime)
  }

  implicit def toAggregatorStat(taxiStat: TaxiStat): AggregatorStat = {
    AggregatorStat(taxiStat.totalAmount, taxiStat.tripDistance, taxiStat.tipAmount)
  }

}
