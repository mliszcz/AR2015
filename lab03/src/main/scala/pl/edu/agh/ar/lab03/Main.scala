package pl.edu.agh.ar.lab03

import scala.util.Random

import org.slf4j.LoggerFactory

import com.typesafe.scalalogging.Logger

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task

import pprint.Config.Defaults._

object Main {

    private val logger = Logger(LoggerFactory.getLogger(this.getClass))

    val prng = new Random(1)

    def main(args: Array[String]) = {

        val taskCount = 3
        val machineCount = 3

        val tasks = Seq.fill(taskCount)(prng.nextDouble)
            .zipWithIndex.map(_.swap).map(Task.tupled).toList

        val machines = (0 to machineCount-1).map(Machine.apply).toList

        val tree = buildMappingTree(tasks, machines)

        println(tasks)
        println(machines)

        tree.foreach(t => {
            pprint.pprintln(t)
        })

    }

    def buildMappingTree(tasks: List[Task],
                         machines: List[Machine]): Option[MappingTree] = {

        def step(task: Task,
                 machine: Machine,
                 parentMapping: Mapping,
                 restTasks: List[Task],
                 usedMachines: List[Machine],
                 freeMachines: List[Machine]): MappingTree = {

            val (newUsedMachines, newFreeMachines) = freeMachines match {
                case Nil => (usedMachines, Nil)
                case next :: rest => (next :: usedMachines, rest)
            }
            println(s"usedMachines: $usedMachines")
            println(s"freeMachines: $freeMachines")

            println(s"newUsedMachines: $newUsedMachines")
            println(s"newFreeMachines: $newFreeMachines")

            println(s"node with $task -> $machine")
            println(s"mapping children $usedMachines")

            val mapping = parentMapping + (task -> machine)

            new MappingTree(mapping,
                            restTasks.headOption.map { nextTask =>
                                newUsedMachines.map { nextMachine =>
                                    step(nextTask,
                                         nextMachine,
                                         mapping,
                                         restTasks.tail,
                                         newUsedMachines,
                                         newFreeMachines)
                                }
                            } getOrElse(Nil))
        }

        (tasks, machines) match {
            case (tasksHead :: tasksTail, machinesHead :: machinesTail) =>
                Some(step(tasksHead,
                          machinesHead,
                          Map(),
                          tasksTail,
                          List(machinesHead),
                          machinesTail))
            case _ => None
        }
    }

//    def buildMappingTree(tasks: List[Task],
//                         target: Machine,
//                         usedMachines: List[Machine],
//                         freeMachines: List[Machine]): MappingTree
//        = tasks match {
//
//        case Nil => new MappingTree(None, Seq())
//        case task :: rest =>
//            val (newFreeMachines, newUsedMachines) = freeMachines match {
//                case Nil => (Nil, usedMachines)
//                case h :: t => (t, h :: usedMachines)
//            }
//            new MappingTree(Some(task -> target),
//                    newUsedMachines.map { machine =>
//                        buildMappingTree(rest,
//                                         machine,
//                                         newUsedMachines,
//                                         newFreeMachines)
//                    })
//    }
}
