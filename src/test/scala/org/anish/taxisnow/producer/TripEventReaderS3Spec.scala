package org.anish.taxisnow.producer

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.typesafe.config.ConfigFactory
import org.mockito.Mockito
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.mockito.{Matchers => mMatchers}

/**
  * Created by anish on 28/12/17.
  */
class TripEventReaderS3Spec extends FlatSpec with Matchers with MockitoSugar {

  private val conf = ConfigFactory.load()
  private val bucket = conf.getString("source.s3.bucket-name")

  "EventReader" should "iterate through lines from S3 objects" in {
    val lineInTripsFile = "Header\n2,2016-01-01 00:00:00,2016-01-01 00:00:00,2,1.10,-73.990371704101563,40.734695434570313,1,N,-73.981842041015625,40.732406616210937,2,7.5,0.5,0.5,0,0,0.3,8.8"

    val s3ObjectSummary = mock[S3ObjectSummary]
    Mockito.doReturn("test-s3-key").when(s3ObjectSummary).getKey
    val objectSummaries = new java.util.ArrayList[S3ObjectSummary]
    objectSummaries.add(s3ObjectSummary)

    val listObjectsV2Result = mock[ListObjectsV2Result]
    Mockito.doReturn(false).when(listObjectsV2Result).isTruncated
    Mockito.doReturn(objectSummaries).when(listObjectsV2Result).getObjectSummaries

    val input = new ByteArrayInputStream(lineInTripsFile.getBytes(StandardCharsets.UTF_8))
    val s3ObjectInputStream = new S3ObjectInputStream(input, null)

    val s3ObjectMock = mock[S3Object]
    Mockito.doReturn(s3ObjectInputStream).when(s3ObjectMock).getObjectContent

    val s3Mock = mock[AmazonS3Client]
    Mockito.doReturn(listObjectsV2Result).when(s3Mock).listObjectsV2(mMatchers.any[ListObjectsV2Request])
    Mockito.doReturn(s3ObjectMock).when(s3Mock).getObject(bucket, "test-s3-key")

    val eventReader = new TripEventReaderS3(s3Mock, conf)
    eventReader.foreach(trip =>
      trip shouldBe TaxiTrip("2016-01-01 00:00:00", "2016-01-01 00:00:00", 1.10, -73.990371704101563, 40.734695434570313, "N", -73.981842041015625, 40.732406616210937)
    )

  }

}
