package org.anish.taxisnow.consumer
package stream

import org.apache.spark.sql.SparkSession

// Goal is to read from Kafka topic and write to CSV

class DemandConsumer {

  val spark = SparkSession
    .builder
    .appName("DemandConsumerToSpark")
    .getOrCreate()

  val df = spark.readStream
    .format("kafka")
    .option("kafka.bootstrap.servers", "localhost:9092")
    .option("subscribe", "taxis_demand")
    .load()

  val query = df.writeStream
    .format("csv")
    .option("path", "~/Desktop/kafka-test")
    .option("checkpointLocation", "~/Desktop/checkpoints")

  // start the streaming
  query.start()
  query.awaitTermination()

}