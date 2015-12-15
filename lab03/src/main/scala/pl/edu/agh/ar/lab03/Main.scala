package pl.edu.agh.ar.lab03

import scala.util.Random
import scala.util.control.Exception._

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

        val taskCount = allCatch opt { args(0).toInt } getOrElse(12)
        val machineCount = allCatch opt {args(1).toInt } getOrElse(5)

        val unitCost = 10.0

        val deadline = allCatch.opt{ args(2).toDouble }
            .getOrElse(unitCost * taskCount / machineCount)


        val workerCount = allCatch opt { args(3).toInt } getOrElse(2)

        val tasks = Seq.fill(taskCount)(prng.nextDouble)
            .map(Math.abs).map(_*10.0)
            .zipWithIndex.map(_.swap).map(Task.tupled).toList

        val machines = (0 to machineCount-1).map(Machine.apply).toList

        ConcurrentSolution.buildMappingTree(tasks,
                                            machines,
                                            deadline,
                                            unitCost,
                                            workerCount)

//        println(s"   tasks: $tasks")
//        println(s"machines: $machines")
    }
}
