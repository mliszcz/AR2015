package lab02

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import scala.annotation.tailrec

object Main {

    type IntMapRDD = RDD[(Int, Int)]

    def makeUndirected(edges: IntMapRDD) =
        (edges ++ edges.map(_.swap)).distinct

    def connectedComponents(graph: IntMapRDD): RDD[Iterable[Int]] = {

        val connections = graph.groupByKey//.cache
        val initWeights = connections map { case (k, _) => (k, k) }

        @tailrec
        def performStep(weights: IntMapRDD): IntMapRDD = {

            val newWeights = connections.join(weights).values.flatMap {
                case (indices, weight) => indices map { (_, weight) }
            } reduceByKey(Math.min)

            val mergedWeights = weights.join(newWeights)
                .mapValues((Math.min _).tupled)

            if (weights.subtract(mergedWeights).count == 0)
                mergedWeights else performStep(mergedWeights)
        }

        performStep(initWeights).map(_.swap).groupByKey.map(_._2)
    }

    def main(args: Array[String]) = {

        val conf = new SparkConf()
            .setAppName("lab02")
            .setMaster("local[4]")

        val sc = new SparkContext(conf)

        val inputGraph = sc.textFile(args(0), 1) map { _.split("\\s+") } map {
            case Array(x: String, y: String) => (x.toInt, y.toInt)
        }

        val graph = makeUndirected(inputGraph)

        println(graph.collectAsMap)

        val result = connectedComponents(graph)

        println(result.collect.toSeq)

        sc.stop()
    }
}
