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
import com.amazonaws.services.glacier.AmazonGlacierClient
import com.amazonaws.services.rds.AmazonRDSClient
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.model.StorageClass
import com.amazonaws.services.glacier.model.CreateVaultRequest
import com.amazonaws.services.glacier.model.DescribeVaultRequest
import com.amazonaws.services.glacier.model.ListVaultsRequest

object Main {

    val credentials = new BasicAWSCredentials(
            "AKIAIHPUABWKEEOLG3EA",
            "SG/KVN8MrIx+0a7iaKguCAcu0Bn9Chm3AGO+fvtJ")

    def main(args: Array[String]) = {

        implicit val s3Client = new AmazonS3Client(credentials)
        implicit val glacierClient = new AmazonGlacierClient(credentials)

        val format = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        val now = LocalDateTime.now.format(format)
        val bucketName = s"tmp-api-$now"

        val mb = allCatch.opt{ args(1) }.map({
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

        val samples = allCatch.opt { args(2).toInt } getOrElse(1)

        val variant = {
            val arg0 = allCatch.opt { args(0) } getOrElse("s3")
            if (List("s3", "rrs", "glacier") contains arg0) arg0 else "s3"
        }

        val ( (uptime, upload), (downtime, download) ) = variant match {
            case v @ ("s3" | "rrs") =>
                val clazz = v match {
                    case "s3" => StorageClass.Standard
                    case "rrs" => StorageClass.ReducedRedundancy
                }
                withS3Bucket(bucketName) {
                    testS3(bucketName, mb, file, samples, clazz)
                }
            case "glacier" =>
                withS3Bucket(bucketName) {
                    testS3(bucketName, mb, file, samples, StorageClass.Glacier)
                }
        }

        println(s"${mb}M | UP   | $uptime s | $upload MBps")
        println(s"${mb}M | DOWN | $downtime s | $download MBps")
    }

    def timeIt(f: => Unit) = {
        val start = System.currentTimeMillis
        f
        (System.currentTimeMillis - start) / 1000.0
    }

    def withS3Bucket[X](bucket: String)(f: => X)
        (implicit client: AmazonS3Client) = {
        client.createBucket(bucket)
        val result = f
        client.listObjects(bucket).getObjectSummaries()
            .map(_.getKey).foreach { client.deleteObject(bucket, _) };
        client.deleteBucket(bucket)
        result
    }

    // http://docs.aws.amazon.com/amazonglacier/latest/dev/downloading-an-archive-using-java.html
    /*
     * Wait for the job to complete.

Most Amazon Glacier jobs take about four hours to complete.
You must wait until the job output is ready for you to download.
If you have either set a notification configuration on the vault identifying
an Amazon Simple Notification Service (Amazon SNS) topic or specified an
Amazon SNS topic when you initiated a job, Amazon Glacier sends a message
to that topic after it completes the job.

You can also poll Amazon Glacier by calling the describeJob method to
determine the job completion status. Although, using an Amazon SNS
topic for notification is the recommended approach.
     */

//    def withGlacierVault[X](bucket: String)(f: => X)
//        (implicit client: AmazonGlacierClient) = {
//        client.createVault(new CreateVaultRequest(bucket))
//        val result = f
//
//        client.listVaults(new ListVaultsRequest()).getVaultList.map {
//            v => v.
//        }
//        client.listObjects(bucket).getObjectSummaries()
//            .map(_.getKey).foreach { client.deleteObject(bucket, _) };
//        client.deleteBucket(bucket)
//        result
//    }

    def testS3(bucket: String,
               mb: Int,
               file: File,
               samples: Int,
               storageClass: StorageClass)
        (implicit client: AmazonS3Client) = {

        val keys = (1 to samples) map { i => s"${mb}M_up_${i}.dat" }

        val upTime = timeIt {
            keys map { key =>
                val req = new PutObjectRequest(bucket, key, file)
                    .withStorageClass(storageClass)
                client.putObject(req)
            }
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

//    def testGlacier()(implicit client: AmazonGlacierClient) = {
//        //
//    }

}
