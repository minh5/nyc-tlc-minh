package org.anish.taxisnow.producer.demand

import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.Config
import org.anish.taxisnow.producer.{MessagingQueue, TaxiTrip}

/**
  * Created by anish on 21/12/17.
  */
class CustomerActor(messagingQueue: MessagingQueue, conf: Config) extends Actor {
  val log = Logging(context.system, this)

  import org.anish.taxisnow.Utils.TaxiTripAddOns

  override def receive = {
    case trip: TaxiTrip =>
      log.debug("Got trip {}", trip)
      if (trip.pickUpTime > System.currentTimeMillis() + 10000) {
        // Taxi hasn't arrived yet. Lets sleep
        Thread.sleep(trip.pickUpTime - System.currentTimeMillis())
      }
      if (trip.pickUpTime < System.currentTimeMillis() + 10000) {
        // He is now seconds away from on boarding a taxi.
        log.info("Sending trip {} to Kafka", trip)
        messagingQueue.enqueueTaxiDemand(TaxiDemandRequest(trip.pickUpTime, trip.pickup_latitude, trip.pickup_longitude))
        Thread.sleep(trip.dropOffTime - trip.pickUpTime) // Sleep in the cab
      }

    case _ => log.error("Invalid Message to customer")
  }
}
