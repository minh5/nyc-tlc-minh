package org.anish.taxisnow.producer.demand

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.anish.taxisnow.producer.TaxiTrip

/**
  * This routes the events to customers
  *
  * Created by anish on 22/12/17.
  */
class CustomerRouter(val props: Props) extends Actor {
  val log = Logging(context.system, this)

  val router: ActorRef = context.actorOf(props, "PooledCustomerRouter")

  def receive = {
    case trip: TaxiTrip =>
      router ! trip
      log.info("Sent Taxi trip {} to customer", trip)
  }
}