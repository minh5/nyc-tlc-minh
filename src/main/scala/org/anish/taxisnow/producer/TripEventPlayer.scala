package org.anish.taxisnow.producer

import akka.actor.ActorRef
import com.typesafe.config.Config

/**
  * This class manages the time of events.
  *
  * Repeats history read from TripEventReader
  *
  * Created by anish on 20/12/17.
  */
class TripEventPlayer(tripEventReader: TripEventReader, conf: Config) {

  import org.anish.taxisnow.Utils.TaxiTripAddOns

  def play(eventProcessorActor: ActorRef) = {
    val playSpeed = conf.getInt("source.play-speed")

    val systemZeroTime = System.currentTimeMillis()
    val eventZeroTime: Long = tripEventReader.take(1).toArray.headOption.get.pickUpTime // This event is ignored

    tripEventReader.foreach { taxiTrip =>
      val eventTimeDifference = taxiTrip.pickUpTime - eventZeroTime // Time passed in the actual logs
      val systemTimeElapsed = (System.currentTimeMillis() - systemZeroTime) * playSpeed // Time passed in our code

      if (systemTimeElapsed < eventTimeDifference) {
        // Our code hasn't caught up till the next event time, hence wait
        // But avoid waiting forever because of anomalies in data ordering
        val sleepTime = math.min((eventTimeDifference - systemTimeElapsed) / playSpeed,
          conf.getLong("source.max-wait-for-next-trip") )
        Thread.sleep(sleepTime)
      }

      // Time to start this trip
      // But lets time travel first!!
      val newPickUpTime = systemZeroTime + (taxiTrip.pickUpTime - eventZeroTime) / playSpeed
      val newDropOffTime = systemZeroTime + (taxiTrip.dropOffTime - eventZeroTime) / playSpeed
      // We can play fast, but the taxis have a min time they are on trip, because they
      // send their locations every 5 seconds when waiting for hire
      val minTripTime = conf.getInt("source.min-trip-time")
      val correctedDropOffTime = if (newDropOffTime - newPickUpTime < minTripTime)
        newPickUpTime + minTripTime
      else newDropOffTime

      // Add the avg speed before time travelling
      val newTrip = taxiTrip.calcActualTravelDuration.timeTravel(newPickUpTime, correctedDropOffTime)

      eventProcessorActor ! newTrip
    }
  }

}
