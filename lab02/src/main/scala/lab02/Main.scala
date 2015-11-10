package lab02

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

object Main {

    // TODO stop algorithm when no change occurs
    val ITERATIONS = 10

    def makeUndirected(edges: RDD[(Int, Int)]) =
        (edges ++ edges.map(_.swap)).distinct

    def connectedComponents(graph: RDD[(Int, Int)]) = {

        val connections: RDD[(Int, Iterable[Int])] = graph.groupByKey.cache
        var weights = connections map { case (k, _) => (k, k) }

        for (i <- 1 to ITERATIONS) {

            val newWeights = connections.join(weights).values.flatMap {
                case (indices, weight) => indices map { (_, weight) }
            } reduceByKey(Math.min)// combineByKey (weight => weight, Math.min, Math.min)

            weights = weights.join(newWeights).mapValues((Math.min _).tupled)
        }

        weights.map(_.swap).groupByKey
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

        println(result.collectAsMap)

        sc.stop()
    }
}
