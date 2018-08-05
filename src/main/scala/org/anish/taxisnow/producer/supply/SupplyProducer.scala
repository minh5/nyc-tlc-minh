package org.anish.taxisnow.producer.supply

import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.routing.SmallestMailboxPool
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.config.ConfigFactory
import org.anish.taxisnow.producer.{TripEventReaderLocalFile, _}
import org.apache.kafka.clients.producer.KafkaProducer

/**
  * This class mimics how taxis on road would be sending their locations to the backend
  *
  * Logic of taxi simulation:
  * A certain number of taxis are defined to start with.
  * Each taxi sends its location every 5 second if it is free.
  *
  * We read data the trip data and queue the trips to the taxi operator's mailbox.
  *
  *
  * Created by anish on 20/12/17.
  */
object SupplyProducer {

  val conf = ConfigFactory.load()

  val totalTaxis = conf.getInt("supply.taxis-on-road")

  def main(args: Array[String]): Unit = {
    // Define the actor system of the taxis
    val system = ActorSystem("CityOfTaxis")
    implicit val ec = system.dispatcher

    val messagingQueue = new KafkaBackend(conf, createKafkaProducer())

    val props = Props(new TaxiActor(messagingQueue, conf)).withRouter(SmallestMailboxPool(nrOfInstances = totalTaxis))
    val taxiOperator = system.actorOf(Props(new TaxiOperator(props)), name = "TaxiOperator")

    // Get the trips available and send to the taxi router.
    val awsCreds = new BasicAWSCredentials(conf.getString("aws.access-key"), conf.getString("aws.secret-key"))
    val amazonS3Client = new AmazonS3Client(awsCreds)

    val source = conf.getString("source.input")
    val tripEventReader = if (source == "S3") // This defines how trips are read from the stored files
      new TripEventReaderS3(amazonS3Client, conf)
    else new TripEventReaderLocalFile(conf)

    val tripEventPlayer = new TripEventPlayer(tripEventReader, conf) // This plays history as it had occurred
    tripEventPlayer.play(taxiOperator)
  }

  private def createKafkaProducer(): KafkaProducer[String, String] = {
    val props = new Properties()
    props.put("bootstrap.servers", conf.getString("kafka.bootstrap-servers"))
    props.put("client.id", "AkkaActorBasedClient")
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](props)
  }
}
