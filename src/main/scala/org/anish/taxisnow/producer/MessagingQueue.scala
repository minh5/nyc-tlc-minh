package org.anish.taxisnow.producer

import org.anish.taxisnow.producer.demand.TaxiDemandRequest
import org.anish.taxisnow.producer.supply.{TaxiForHireRequest, TripCompleteRequest}

import scala.concurrent.{ExecutionContext, Future}

import com.typesafe.config.Config
import org.apache.kafka.clients.producer.{Producer, ProducerRecord}
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.write


/**
  * This class gets a trip and sends to the backend implemented with Kafka or Kinesis
  *
  * Created by anish on 20/12/17.
  */
abstract class MessagingQueue(implicit executionContext: ExecutionContext) {

  def enqueueTaxiForHire(taxiForHireRequest: TaxiForHireRequest): Future[Unit]

  def enqueueTaxiDemand(taxiDemandRequest: TaxiDemandRequest): Future[Unit]

  def tripComplete(tripCompleteRequest: TripCompleteRequest): Future[Unit]

}

class KafkaBackend(conf: Config, kafkaProducer: Producer[String, String])(implicit executionContext: ExecutionContext) extends MessagingQueue {
  val supplyTopic = conf.getString("kafka.supply-topic")
  val demandTopic = conf.getString("kafka.demand-topic")
  val tripCompleteTopic = conf.getString("kafka.trip-complete-topic")

  implicit val formats = Serialization.formats(NoTypeHints)

  override def enqueueTaxiForHire(taxiForHireRequest: TaxiForHireRequest): Future[Unit] = Future {
    sendCaseClassAsJson(taxiForHireRequest, supplyTopic)
  }

  override def enqueueTaxiDemand(taxiDemandRequest: TaxiDemandRequest): Future[Unit] = Future {
    sendCaseClassAsJson(taxiDemandRequest, demandTopic)
  }

  override def tripComplete(tripCompleteRequest: TripCompleteRequest): Future[Unit] = Future {
    sendCaseClassAsJson(tripCompleteRequest, tripCompleteTopic)
  }

  private def sendCaseClassAsJson(request: BackendRequest, topic: String) = {
    Some(request)
      .map(x => write(x)) // convert to JSON
      .map(new ProducerRecord[String, String](topic, _)) // Convert to Producer Record
      .foreach(kafkaProducer.send)
  }

}

