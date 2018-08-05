package org.anish.taxisnow.producer.supply

import java.util.UUID

import akka.actor.Actor
import akka.event.Logging
import com.typesafe.config.Config
import org.anish.taxisnow.producer.{MessagingQueue, TaxiTrip}

import scala.concurrent.{ExecutionContext, Future}

/**
  * An instance of this actor acts like a Taxi on road.
  *
  * Created by anish on 20/12/17.
  */
class TaxiActor(messagingQueue: MessagingQueue, conf: Config) extends Actor {
  val log = Logging(context.system, this)
  implicit val ec = ExecutionContext.global // This is a different EC used for creating the futures

  val taxiId = UUID.randomUUID().toString // This is a part of this Taxi Actor, and would be different for each instance

  @volatile var state: TaxiStates = TaxiForHire // TODO check for resource contention

  @volatile var currentLat: Double = 0.0
  @volatile var currentLon: Double = 0.0

  import org.anish.taxisnow.Utils.TaxiTripAddOns

  override def preStart(): Unit = {
    super.preStart()
    val taxiForHireFuture = showTaxiForHire()
  }

  override def receive = {
    case trip: TaxiTrip =>
      log.info(s"Received Trip $trip")
      /*
      Once a taxi trip arrives, it checks whether it has passed the pickup time or not. If passed
      it changes state and sleeps till drop off

      Once it reaches drop off time, it starts sending its drop off location to the backend every 5 second,
      till the next Trip event arrives.
       */

      if (System.currentTimeMillis() < trip.pickUpTime) {
        // Its not yet time to start the trip.
        // We are waiting for customer to come? // We are still free and for hire
        println("Waiting for customer")
        currentLat = trip.pickup_latitude
        currentLon = trip.pickup_longitude
        Thread.sleep(trip.pickUpTime - System.currentTimeMillis())
      }
      if (System.currentTimeMillis() >= trip.pickUpTime) {
        // Its time Or Past.
        // Lets go on a trip
        state = TaxiHired
        log.info(state + " : " + taxiId)

        // Now we will sleep through the ride.
        Thread.sleep(trip.dropOffTime - trip.pickUpTime)

        currentLat = trip.dropoff_latitude
        currentLon = trip.dropoff_longitude
        state = TaxiForHire
      }

      // The trip is complete now
      val tripTime = trip.dropOffTime - trip.pickUpTime
      val tripCompleteReq = TripCompleteRequest(taxiId, trip.trip_distance, tripTime, trip.store_and_fwd_flag,
        trip.pickup_latitude, trip.pickup_longitude, trip.dropoff_latitude, trip.dropoff_longitude, trip.actualTravelDuration)
      messagingQueue.tripComplete(tripCompleteReq)
      log.info(s"Trip completed $trip")
    case _ => log.error("Invalid Message to taxi actor")
  }

  def showTaxiForHire(): Future[Unit] = Future {
    // Check every 5 second if this taxi is free
    while (true) {
      if (state == TaxiForHire) {
        messagingQueue.enqueueTaxiForHire(TaxiForHireRequest(taxiId, System.currentTimeMillis(), currentLat, currentLon))
      }
      Thread.sleep(conf.getLong("supply.time-between-taxi-for-hire-requests"))
    }
  }
}

sealed abstract class TaxiStates

case object TaxiForHire extends TaxiStates

case object TaxiHired extends TaxiStates



