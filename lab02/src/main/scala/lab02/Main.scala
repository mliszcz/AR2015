package lab02

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.spark.HashPartitioner
import scala.annotation.tailrec
import scala.util.control.Exception._

object Main {

    def mean(xs: Seq[Long]): Double = xs match {
        case Seq() => 0.0
        case ys => ys.reduceLeft(_ + _) / ys.size.toDouble
    }

    def stddev(xs: Seq[Long], avg: Double): Double = xs match {
        case Seq() => 0.0
        case ys => Math.sqrt((0.0 /: ys) {
            (a,e) => a + Math.pow(e - avg, 2.0)
        } / xs.size)
    }

    type IntMapRDD = RDD[(Int, Int)]

    def makeUndirected(edges: IntMapRDD) =
        (edges ++ edges.map(_.swap)).distinct

    def connectedComponents(graph: IntMapRDD): RDD[Iterable[Int]] = {

        val connections = graph.groupByKey
//            .partitionBy(new HashPartitioner(2))
            .cache
        val initWeights = connections map { case (k, _) => (k, k) } cache

        @tailrec
        def performStep(weights: IntMapRDD): IntMapRDD = {

            val newWeights = connections.join(weights).values.flatMap {
                case (indices, weight) => indices map { (_, weight) }
            } reduceByKey(Math.min)

            val mergedWeights = weights.join(newWeights)
                .mapValues((Math.min _).tupled) cache

            if (weights.subtract(mergedWeights).count == 0)
                mergedWeights else performStep(mergedWeights)
        }

        performStep(initWeights).map(_.swap).groupByKey.map(_._2)
    }

    def timeIt[R](f: => R): (Long, R) = {
        val start = System.currentTimeMillis
        val result = f
        val delta = System.currentTimeMillis - start
        (delta, result)
    }

    def runSeries(graph: IntMapRDD, series: Int) = timeIt {
        (1 to series) foreach { _ => connectedComponents(graph) }
    } _1

    def runSeriesMeanDev(graph: IntMapRDD, series: Int) = {
        val times = (1 to series) map { _ =>
            timeIt { connectedComponents(graph) }
        } map(_._1)
        val time = mean(times)
        val timedev = stddev(times, time)
        (time, timedev)
    }

    def main(args: Array[String]) = {

        // usage: <local|cluster> <local-nodes> <series> <generate|'file'> [v]

        implicit class SparkConfWithLocalOrRemote(conf: SparkConf) {
            def localOrRemote() =
                allCatch.opt { args(0) == "local" }
                .filter { _ == true } map { _ =>
                    val nodes = allCatch.opt { args(1).toInt } getOrElse (4)
                    conf.setMaster(s"local[$nodes]")
                } getOrElse conf
        }

        val conf = new SparkConf()
            .setAppName("lab02")
            .localOrRemote

        val sc = new SparkContext(conf)

        val optionalGraph = allCatch.opt { args(3) } map {
            case "generate" =>
                val vertices = allCatch.opt { args(4).toInt } getOrElse 100
                GraphGenerators.logNormalGraph(sc, vertices, seed = 1).edges.map {
                    edge => (edge.srcId.toInt, edge.dstId.toInt)
                }
            case filePath =>
                sc.textFile(filePath, 1) filter { ! _.startsWith("#")
                } map { _.split("\\s+") } map {
                    case Array(x: String, y: String) => (x.toInt, y.toInt)
                }
        } map { _.cache }

        optionalGraph foreach { inputGraph =>

            val graph = makeUndirected(inputGraph).cache

//            println(graph.collectAsMap)
//            val result = connectedComponents(graph)
//            println(result.collect.toSeq)

            val series = allCatch.opt { args(2).toInt } getOrElse(4)

//            val time = runSeries(graph, series)
//            println(time / 1000.0 / series)

            val (time, timedev) = runSeriesMeanDev(graph, series)
            println(s"${time / 1000.0} ${timedev / 1000.0}")
        }

        sc.stop()
    }
}
