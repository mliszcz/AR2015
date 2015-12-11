package pl.edu.agh.ar.lab03.sequental

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task

object SequentialSolution {

        def buildMappingTree(tasks: List[Task],
                         machines: List[Machine],
                         deadline: Double,
                         unitCost: Double) = {

        def evaluateSolution(mapping: Mapping) =
            mapping.groupBy(_._2).mapValues(_.map(_._1)).values
            .foldLeft((0.0, 0.0))({
                case ((maxTimespan, totalCost), tasks) =>
                    val time = tasks.map(_.duration).sum
                    val cost = Math.ceil(time) * unitCost
                    (Math.max(time, maxTimespan), totalCost + cost)
                })

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

            val mapping = parentMapping + (task -> machine)
            val machines = mapping.groupBy(_._2).keySet
            val (maxTimespan, totalCost) = evaluateSolution(mapping)

            // no more tasks - print solution
            if (restTasks.size == 0) {
                println(s"""
                    |solution accepted:
                    |machines=${machines.size}
                    |timespan=$maxTimespan
                    |totalCost=$totalCost
                    |mapping=$mapping
                    """.stripMargin.replaceAll("\n", " "))
            }

            new MappingTree(
                    mapping,
                    restTasks.headOption.map { nextTask =>
                        newUsedMachines
                            .filter { nextMachine =>
                                val nextMapping = mapping +
                                        (nextTask -> nextMachine)
                                val (time, _) = evaluateSolution(nextMapping)

                                if (time >= deadline) {
                                    println(s"""ignoring tasks with
                                        |${restTasks.tail.length} remaining
                                        """.stripMargin.replaceAll("\n", " "))
                                }

                                time < deadline
                             }
                            .map { nextMachine =>
                                step(nextTask,
                                     nextMachine,
                                     mapping,
                                     restTasks.tail,
                                     newUsedMachines,
                                     newFreeMachines)
                            }
                        } getOrElse(Nil))
        }

        val tree = (tasks, machines) match {
            case (tasksHead :: tasksTail, machinesHead :: machinesTail) =>
                Some(step(tasksHead,
                          machinesHead,
                          Map(),
                          tasksTail,
                          List(machinesHead),
                          machinesTail))
            case _ => None
        }

        tree.foreach(t => {
            pprint.pprintln(t)
        })
    }
}
