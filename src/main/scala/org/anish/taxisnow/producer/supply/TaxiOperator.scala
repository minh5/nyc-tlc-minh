package org.anish.taxisnow.producer.supply

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.anish.taxisnow.producer.TaxiTrip

/**
  * This actor basically acts as a Router sending trips to different TaxiActors
  *
  * Created by anish on 20/12/17.
  */
class TaxiOperator(val props: Props) extends Actor {
  val log = Logging(context.system, this)

  val router: ActorRef = context.actorOf(props, "PooledTaxiRouter")

  def receive = {
    case trip: TaxiTrip =>
      router ! trip
      log.info("Sent Taxi trip {} ", trip)
  }
}