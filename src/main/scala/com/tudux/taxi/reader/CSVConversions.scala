package com.tudux.taxi.reader

import com.tudux.taxi.actors.loader.TaxiTripEntry

import java.util.UUID

object CSVConversions {

  def fromCsvEntryToCaseClass(csvEntry: String): TaxiTripEntry = {
    val arrayString = csvEntry.split(",")
    println(s"Trying to parse $csvEntry")
    TaxiTripEntry(
      arrayString(0).toInt,
      arrayString(1).toString,
      arrayString(2).toString,
      arrayString(3).toInt,
      arrayString(4).toDouble,
      arrayString(5).toDouble,
      arrayString(6).toDouble,
      arrayString(7).toInt,
      arrayString(8).toString,
      arrayString(9).toDouble,
      arrayString(10).toDouble,
      arrayString(11).toInt,
      arrayString(12).toDouble,
      arrayString(13).toDouble,
      arrayString(14).toDouble,
      arrayString(15).toDouble,
      arrayString(16).toDouble,
      arrayString(17).toDouble,
      arrayString(18).toDouble
    )
  }

  def addUUID(taxiTripEntry: TaxiTripEntry): (String, TaxiTripEntry) = {
    val uuid = UUID.randomUUID().toString
    (uuid, taxiTripEntry)
  }

}
