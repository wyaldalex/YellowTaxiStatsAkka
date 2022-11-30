package com.tudux.taxi.reader

import com.tudux.taxi.actors.loader.TaxiTripEntry
import com.tudux.taxi.reader.CSVConversions.fromCsvEntryToCaseClass
import org.scalatest.GivenWhenThen
import org.scalatest.featurespec.AnyFeatureSpecLike
import org.scalatest.matchers.should.Matchers

class CSVConversionsSpec extends AnyFeatureSpecLike with GivenWhenThen with Matchers{
  info("As a external module")
  info("I should be able to handle CSV conversions operations")
  info("Consequently the conversion from  CSV to Class should correspond")

  Feature("handle CSV conversion") {
    Scenario("Convert CSV String to class") {
      Given("a TaxiTripEntry and String csvEntry")
      val aTaxiTripEntry: TaxiTripEntry = TaxiTripEntry(vendorID = 1,
        tpepPickupDatetime = "2015-01-15 19:05:42", tpepDropoffDatetime = "2015-01-15 19:16:18",
        passengerCount = 1, tripDistance = 1.53, pickupLongitude = 180, pickupLatitude = 90, rateCodeID = 1,
        storeAndFwdFlag = "Y", dropoffLongitude = 180, dropoffLatitude = 90, paymentType = 2,
        fareAmount = 9, extra = 0, mtaTax = 0, tipAmount = 0, tollsAmount = 0, improvementSurcharge = 0,
        totalAmount = 2.0)
      val csvEntry: String = "1,2015-01-15 19:05:42,2015-01-15 19:16:18,1,1.53,180,90,1,Y,180,90,2,9,0,0,0,0,0,2.0"

      When("A conversion happens")
      val aConversionTaxiTripEntry: TaxiTripEntry = fromCsvEntryToCaseClass(csvEntry)

      Then("aConversionTaxiTripEntry should be equal to aTaxiTripEntry")
      aConversionTaxiTripEntry shouldBe aTaxiTripEntry
    }
  }
}
