package lab02

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import scala.annotation.tailrec
import scala.Numeric._

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

        val connections = graph.groupByKey.cache
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

        val conf = new SparkConf()
            .setAppName("lab02")
            .setMaster("local[4]")

        val sc = new SparkContext(conf)

        val inputGraph = sc.textFile(args(0), 1) map { _.split("\\s+") } map {
            case Array(x: String, y: String) => (x.toInt, y.toInt)
        } cache

        val graph = makeUndirected(inputGraph).cache

//        println(graph.collectAsMap)
//        val result = connectedComponents(graph)
//        println(result.collect.toSeq)

        val series = 50

//        val time = runSeries(graph, series)
//        println(time / 1000.0 / series)

        val (time, timedev) = runSeriesMeanDev(graph, series)
        println(s"${time / 1000.0} ${timedev / 1000.0}")

        sc.stop()
    }
}
