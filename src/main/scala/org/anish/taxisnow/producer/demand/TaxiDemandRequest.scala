package org.anish.taxisnow.producer.demand

import org.anish.taxisnow.producer.BackendRequest

/**
  * Created by anish on 21/12/17.
  */
case class TaxiDemandRequest(eventTime: Long,
                             lat: Double, lon: Double)
  extends BackendRequest

