package pl.edu.agh.ar.lab03

import scala.util.Random

import org.slf4j.LoggerFactory

import com.typesafe.scalalogging.Logger

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task
import pl.edu.agh.ar.lab03.sequental.SequentialSolution
import pl.edu.agh.ar.lab03.concurrent.ConcurrentSolution

import pprint.Config.Defaults._

object Main {

    private val logger = Logger(LoggerFactory.getLogger(this.getClass))

    val prng = new Random(1)

    def main(args: Array[String]) = {

        val taskCount = 13//6
        val machineCount = 5//3

        val deadline = 30.0
        val unitCost = 10.0

        val tasks = Seq.fill(taskCount)(prng.nextGaussian)
            .map(Math.abs).map(_*10.0)
            .zipWithIndex.map(_.swap).map(Task.tupled).toList

        val machines = (0 to machineCount-1).map(Machine.apply).toList

        ConcurrentSolution.buildMappingTree(tasks,
                                            machines,
                                            deadline,
                                            unitCost)

        println(s"   tasks: $tasks")
        println(s"machines: $machines")
    }
}
