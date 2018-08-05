package org.anish.taxisnow.producer.demand

import java.util.Properties

import akka.actor.{ActorSystem, Props}
import akka.routing.SmallestMailboxPool
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.config.ConfigFactory
import org.anish.taxisnow.producer._
import org.apache.kafka.clients.producer.KafkaProducer

/**
  * Main class for the actor system which simulates data from customers.
  *
  * Created by anish on 21/12/17.
  */
object DemandProducer {

  val conf = ConfigFactory.load()

  val totalCustomersOnRoad = conf.getInt("demand.customers-on-road")

  def main(args: Array[String]): Unit = {
    // Define the actor system of the taxis
    val system = ActorSystem("CityOfRiders")
    implicit val ec = system.dispatcher

    val messagingQueue = new KafkaBackend(conf, createKafkaProducer())

    val props = Props(new CustomerActor(messagingQueue, conf)).withRouter(SmallestMailboxPool(nrOfInstances = totalCustomersOnRoad))
    val customerRouter = system.actorOf(Props(new CustomerRouter(props)), name = "CustomerRouter")

    // Get the trips available and send to the taxi router.
    val awsCreds = new BasicAWSCredentials(conf.getString("aws.access-key"), conf.getString("aws.secret-key"))
    val amazonS3Client = new AmazonS3Client(awsCreds)

    val source = conf.getString("source.input")
    val tripEventReader = if (source == "S3") // This defines how trips are read from the stored files
      new TripEventReaderS3(amazonS3Client, conf)
    else new TripEventReaderLocalFile(conf)

    val tripEventPlayer = new TripEventPlayer(tripEventReader, conf) // This plays history as it had occurred
    tripEventPlayer.play(customerRouter)
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
