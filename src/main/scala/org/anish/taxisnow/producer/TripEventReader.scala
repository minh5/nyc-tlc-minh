package org.anish.taxisnow.producer

import java.io.{BufferedReader, InputStreamReader}

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{ListObjectsV2Request, ListObjectsV2Result, S3ObjectSummary}
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import com.typesafe.config.Config
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * The parser for the source files into a TaxiTrip Event
  *
  * Created by anish on 20/12/17.
  */
abstract class TripEventReader extends Iterator[TaxiTrip]

/**
  * Stream a file from S3. Doesn't load the whole file in memory.
  *
  * @param s3
  * @param conf
  */
class TripEventReaderS3(s3: AmazonS3, conf: Config) extends TripEventReader {

  private val logger = LoggerFactory.getLogger(classOf[TripEventReaderS3])
  val bucket = conf.getString("source.s3.bucket-name")
  val filePathPrefix = conf.getString("source.s3.filePathPrefix")

  lazy val s3Keys = {
    val fileList = Try {
      val req = new ListObjectsV2Request().withBucketName(bucket)
        .withPrefix(filePathPrefix).withMaxKeys(1000)

      val allRes: java.util.List[S3ObjectSummary] = new java.util.ArrayList[S3ObjectSummary]()
      var res: ListObjectsV2Result = null
      do {
        res = s3.listObjectsV2(req)
        allRes.addAll(res.getObjectSummaries)
        logger.debug("Next Continuation Token : " + res.getNextContinuationToken)
        req.setContinuationToken(res.getNextContinuationToken)
      } while (res.isTruncated)
      allRes
    } match {
      case Success(x) => x
      case Failure(ase: AmazonServiceException) =>
        val msg = "Caught an AmazonServiceException, which means your request made it " +
          "to Amazon S3, but was rejected with an error response for some reason."
        logger.error(msg)
        logger.error("Error Message:    " + ase.getMessage)
        logger.error("HTTP Status Code: " + ase.getStatusCode)
        logger.error("AWS Error Code:   " + ase.getErrorCode)
        logger.error("Error Type:       " + ase.getErrorType)
        logger.error("Request ID:       " + ase.getRequestId)

        throw ase
      case Failure(ace: AmazonClientException) =>
        val msg = "Caught an AmazonClientException, " + "which means the client encountered " +
          "an internal error while trying to communicate" + " with S3, " +
          "such as not being able to access the network."
        logger.error(msg)
        logger.error("Error Message: " + ace.getMessage)
        throw ace
      case Failure(exp) => throw exp
    }
    val files = fileList.asScala.map(_.getKey).filter(!_.endsWith("_SUCCESS")).sorted // Ignore _SUCCESS Files
    logger.warn("Files to read {}", files)
    files.toIterator
  }

  var br: BufferedReader = _
  var line: String = _

  // Check if the current file is open or open another file
  override def hasNext: Boolean = {
    if (line == null) {
      // Move to the next file
      if (s3Keys.hasNext) {
        val s3InputStream = s3.getObject(bucket, s3Keys.next()).getObjectContent
        br = new BufferedReader(new InputStreamReader(s3InputStream))
        br.readLine() // Ignore Header of each file
      }
    }
    if (br != null) // 0 files
      line = br.readLine()
    line != null
  }

  override def next(): TaxiTrip = {
    lineReader(line)
  }


  private def lineReader(line: String): TaxiTrip = {
    val Array(_,
    tpep_pickup_datetime,
    tpep_dropoff_datetime,
    _,
    trip_distance,
    pickup_longitude,
    pickup_latitude,
    _,
    store_and_fwd_flag,
    dropoff_longitude,
    dropoff_latitude,
    _, _, _, _, _, _, _, _) = line.split(",")

    TaxiTrip(tpep_pickup_datetime,
      tpep_dropoff_datetime,
      trip_distance.toDouble,
      pickup_longitude.toDouble,
      pickup_latitude.toDouble,
      store_and_fwd_flag,
      dropoff_longitude.toDouble,
      dropoff_latitude.toDouble)
  }
}

/**
  * Read from a local file during dev
  *
  * NOTE: This is only used for dev, and is not tested.
  *
  * @param conf
  */
class TripEventReaderLocalFile(conf: Config) extends TripEventReader {

  val tripsFilePath = conf.getString("source.local.taxi-trips-sorted-data")

  val fileStrIterator = Source.fromFile(tripsFilePath).getLines()

  var header = true

  override def hasNext: Boolean = fileStrIterator.hasNext // TODO Check and ignore header

  override def next(): TaxiTrip = {
    if (header) {
      fileStrIterator.next()
      header = false
    }

    val Array(_,
    tpep_pickup_datetime,
    tpep_dropoff_datetime,
    _,
    trip_distance,
    pickup_longitude,
    pickup_latitude,
    _,
    store_and_fwd_flag,
    dropoff_longitude,
    dropoff_latitude,
    _, _, _, _, _, _, _, _) = fileStrIterator.next().split(",")

    TaxiTrip(tpep_pickup_datetime,
      tpep_dropoff_datetime,
      trip_distance.toDouble,
      pickup_longitude.toDouble,
      pickup_latitude.toDouble,
      store_and_fwd_flag,
      dropoff_longitude.toDouble,
      dropoff_latitude.toDouble)
  }
}

