package org.anish.taxisnow.producer.supply

import org.anish.taxisnow.producer.BackendRequest

/**
  * Created by anish on 21/12/17.
  */
case class TaxiForHireRequest(taxiId: String,
                              eventTime: Long,
                              lat: Double, lon: Double)
  extends BackendRequest
