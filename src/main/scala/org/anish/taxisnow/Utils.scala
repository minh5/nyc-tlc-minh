package org.anish.taxisnow

import java.util.Date

import ch.hsr.geohash.GeoHash
import org.anish.taxisnow.producer.TaxiTrip
import org.anish.taxisnow.producer.demand.TaxiDemandRequest
import org.anish.taxisnow.producer.supply.{TaxiForHireRequest, TripCompleteRequest}

/**
  * Created by anish on 21/12/17.
  */
object Utils {

  implicit class TaxiTripAddOns(taxiTrip: TaxiTrip) {
    private val DATE_FORMAT = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

    def pickUpTime: Long = DATE_FORMAT.parse(taxiTrip.tpep_pickup_datetime.trim).getTime

    def dropOffTime: Long = DATE_FORMAT.parse(taxiTrip.tpep_dropoff_datetime.trim).getTime

    def calcActualTravelDuration: TaxiTrip = {
      // This is for traffic congestion calculator
      taxiTrip.copy(actualTravelDuration = taxiTrip.dropOffTime - taxiTrip.pickUpTime) // in milliseconds
    }

    def timeTravel(pickUpTime: Long, dropOffTime: Long): TaxiTrip = {
      taxiTrip.copy(
        tpep_pickup_datetime = DATE_FORMAT.format(new Date(pickUpTime)),
        tpep_dropoff_datetime = DATE_FORMAT.format(new Date(dropOffTime))
      )
    }
  }

  val geoHashLevel = 6

  implicit class TaxiForHireRequestAddOns(taxiForHireRequest: TaxiForHireRequest) {
    def geoHash: String =
      GeoHash.withCharacterPrecision(taxiForHireRequest.lat, taxiForHireRequest.lon, geoHashLevel)
        .toBase32
  }

  implicit class TaxiDemandRequestAddOns(taxiDemandRequest: TaxiDemandRequest) {
    def geoHash: String =
      GeoHash.withCharacterPrecision(taxiDemandRequest.lat, taxiDemandRequest.lon, geoHashLevel)
        .toBase32
  }

  implicit class TripCompleteRequestAddOns(tripCompleteRequest: TripCompleteRequest) {
    def pickupGeoHash: String =
      GeoHash.withCharacterPrecision(tripCompleteRequest.pickup_latitude,
        tripCompleteRequest.pickup_longitude, geoHashLevel)
        .toBase32

    def dropoffGeoHash: String =
      GeoHash.withCharacterPrecision(tripCompleteRequest.dropoff_latitude,
        tripCompleteRequest.dropoff_longitude, geoHashLevel)
        .toBase32

  }

}
