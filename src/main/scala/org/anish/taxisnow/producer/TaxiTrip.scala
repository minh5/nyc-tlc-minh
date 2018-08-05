package org.anish.taxisnow.producer

/**
  * Created by anish on 20/12/17.
  */
case class TaxiTrip(tpep_pickup_datetime: String,
                    tpep_dropoff_datetime: String,
                    trip_distance: Double,
                    pickup_longitude: Double,
                    pickup_latitude: Double,
                    store_and_fwd_flag: String,
                    dropoff_longitude: Double,
                    dropoff_latitude: Double,
                    actualTravelDuration: Long = 0L) // This will be added later, since it is not in file
