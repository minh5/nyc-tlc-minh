package org.anish.taxisnow.producer.supply

import org.anish.taxisnow.producer.BackendRequest

/**
  * Created by anish on 26/12/17.
  */
case class TripCompleteRequest(taxiId: String,
                               tripDistance: Double,
                               travelTime: Long,
                               storeForwardFlag: String,
                               pickup_latitude: Double,
                               pickup_longitude: Double,
                               dropoff_latitude: Double,
                               dropoff_longitude: Double,
                               actualTravelDuration: Long)
  extends BackendRequest
