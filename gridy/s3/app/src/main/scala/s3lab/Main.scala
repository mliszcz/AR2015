package s3lab

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.amazonaws.services.s3.model.DeleteBucketRequest
import scala.collection.JavaConversions._
import com.amazonaws.services.s3.model.GetObjectRequest
import scala.util.control.Exception._

object Main {

    val credentials = new BasicAWSCredentials(
            "AKIAIHPUABWKEEOLG3EA",
            "SG/KVN8MrIx+0a7iaKguCAcu0Bn9Chm3AGO+fvtJ")

    def main(args: Array[String]) = {

        implicit val client = new AmazonS3Client(credentials)

        val format = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val now = LocalDateTime.now.format(format)
        val bucketName = s"tmp-api-$now"

        client.createBucket(bucketName)

        val mb = allCatch.opt{ args(0) }.map({
            case "1M" => 1
            case "10M" => 10
            case "100M" => 100
            case _ => 1
        }).getOrElse(1)

        val file = new File(mb match {
            case 1 => "../data/1M.dat"
            case 10 => "../data/10M.dat"
            case 100 => "../data/100M.dat"
        })

        val samples = allCatch.opt { args(1).toInt } getOrElse(1)

        val ( (uptime, upload), (downtime, download) )
            = testS3(bucketName, mb, file, samples)

        println(s"${mb}M | UP   | $uptime s | $upload MBps")
        println(s"${mb}M | DOWN | $downtime s | $download MBps")

        client.listObjects(bucketName).getObjectSummaries()
            .map(_.getKey).foreach { client.deleteObject(bucketName, _) };

        client.deleteBucket(bucketName)


    }

    def timeIt(f: => Unit) = {
        val start = System.currentTimeMillis
        f
        (System.currentTimeMillis - start) / 1000.0
    }

    def testS3(bucket: String, mb: Int, file: File, samples: Int)
        (implicit client: AmazonS3Client) = {

        val keys = (1 to samples) map { i => s"${mb}M_up_${i}.dat" }

        val upTime = timeIt {
            keys map { client.putObject(bucket, _, file) }
        }

        val upload = (samples*mb)/upTime

        val downTime = timeIt {
            keys map { key =>
                val dest = File.createTempFile(key, "")
                val req = new GetObjectRequest(bucket, key)
                client.getObject(req, dest)
            }
        }

        val download = (samples*mb)/downTime

        ( (upTime, upload), (downTime, download) )
    }
}
