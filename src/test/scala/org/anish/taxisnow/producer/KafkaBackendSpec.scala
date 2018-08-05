package org.anish.taxisnow.producer

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.config.ConfigFactory
import org.anish.taxisnow.producer.demand.TaxiDemandRequest
import org.anish.taxisnow.producer.supply.{TaxiForHireRequest, TripCompleteRequest}
import org.apache.kafka.clients.producer.{KafkaProducer, MockProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import org.mockito.{Mockito, Matchers => mMatchers}
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.mock.MockitoSugar

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by anish on 28/12/17.
  */
class KafkaBackendSpec extends FlatSpec with Matchers with MockitoSugar {

  private val conf = ConfigFactory.load()

  import scala.concurrent.ExecutionContext.Implicits.global

  "Kafka Backend" should "send taxi for hire request" in {
    val currentTime = System.currentTimeMillis()
    val taxiForHireRequest = TaxiForHireRequest("taxi-id", currentTime, 56.6, 110.0)

    val mockProducer = new MockProducer[String, String](true, new StringSerializer(), new StringSerializer())
    val kafkaBackend = new KafkaBackend(conf, mockProducer)

    val kafkaRequestFuture = kafkaBackend.enqueueTaxiForHire(taxiForHireRequest)
    Await.ready(kafkaRequestFuture, 5 seconds)

    mockProducer.history().size() shouldBe 1
    mockProducer.history().get(0).topic() shouldBe conf.getString("kafka.supply-topic")
  }

  "Kafka Backend" should "send demand request" in {
    val currentTime = System.currentTimeMillis()
    val taxiDemandRequest = TaxiDemandRequest(currentTime, 34.5, 32.4)

    val mockProducer = new MockProducer[String, String](true, new StringSerializer(), new StringSerializer())
    val kafkaBackend = new KafkaBackend(conf, mockProducer)

    val kafkaRequestFuture = kafkaBackend.enqueueTaxiDemand(taxiDemandRequest)
    Await.ready(kafkaRequestFuture, 5 seconds)

    mockProducer.history().size() shouldBe 1
    mockProducer.history().get(0).topic() shouldBe conf.getString("kafka.demand-topic")
  }

  "Kafka Backend" should "send trip complete" in {
    val tripCompleteRequest = TripCompleteRequest("taxi-id", 2.3, 14000L, "N", 45.56, -90.39, 45.57, -90.40, 2840000)

    val mockProducer = new MockProducer[String, String](true, new StringSerializer(), new StringSerializer())
    val kafkaBackend = new KafkaBackend(conf, mockProducer)

    val kafkaRequestFuture = kafkaBackend.tripComplete(tripCompleteRequest)
    Await.ready(kafkaRequestFuture, 5 seconds)

    mockProducer.history().size() shouldBe 1
    mockProducer.history().get(0).topic() shouldBe conf.getString("kafka.trip-complete-topic")
  }

}
