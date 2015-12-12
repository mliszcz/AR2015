package pl.edu.agh.ar.lab03.concurrent

import akka.actor.Actor
import akka.actor.ActorRef

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task
import pl.edu.agh.ar.lab03.concurrent.Messages._

class TreeWorker(
    val master: ActorRef,
    val tasks: List[Task],
    val machines: List[Machine],
    val deadline: Double,
    val unitCost: Double) extends Actor {

    val STEPS = 5

    var totalWork = 0
    var discardedWork = 0

    master ! WorkRequestMsg()

    def receive = {

        case WorkAssignmentMsg(assignedWork) =>
            master ! WorkDoneMsg(doWork(assignedWork, STEPS))

        case WorkDoneAckMsg() =>
            master ! WorkRequestMsg()

        case ReportRequestMsg() =>
            sender ! ReportResponseMsg(totalWork, discardedWork)
    }

    def evaluateSolution(mapping: Mapping) =
        mapping.groupBy(_._2).mapValues(_.map(_._1)).values
        .foldLeft((0.0, 0.0))({
            case ((maxTimespan, totalCost), tasks) =>
                val time = tasks.map(_.duration).sum
                val cost = Math.ceil(time) * unitCost
                    (Math.max(time, maxTimespan), totalCost + cost)
        })

    /** Do work. Either solution is found or there is more work. */
    def doWorkStep(assignedWork: WorkDescription)
    : Either[Seq[WorkDescription], Mapping] = {

        totalWork += 1

        val task = assignedWork.task
        val machine = assignedWork.machine
        val parentMapping = assignedWork.parentMapping
        val restTasks = assignedWork.restTasks
        val usedMachines = assignedWork.usedMachines
        val freeMachines = assignedWork.freeMachines

        // try using one more machine
        val (newUsedMachines, newFreeMachines) = freeMachines match {
                case Nil => (usedMachines, Nil)
                case next :: rest => (next :: usedMachines, rest)
        }

        val mapping = parentMapping + (task -> machine)

        if (restTasks.size == 0) {
            // solution found
            Right(mapping)
        } else {
            // more tasks
            val nextTask = restTasks.head

            val moreWork = newUsedMachines.filter { nextMachine =>
                val nextMapping = mapping + (nextTask -> nextMachine)
                val (time, _) = evaluateSolution(nextMapping)
                if (time >= deadline) {
                    discardedWork += 1
//                    println(s"""ignoring tasks with
//                    |${restTasks.tail.length} remaining
//                    """.stripMargin.replaceAll("\n", " "))
                }

                time < deadline
            }
            .map { nextMachine =>
                WorkDescription(nextTask,
                                nextMachine,
                                mapping,
                                restTasks.tail,
                                newUsedMachines,
                                newFreeMachines)
            }

            Left(moreWork)
        }
    }

    def doWork(assignedWork: WorkDescription, steps: Int)
    : WorkResult = {

        val (moreWork, mapping) = doWorkStep(assignedWork) match {
            case Left(moreWork) => (moreWork, Nil)
            case Right(mapping) => (Nil, Seq(mapping))
        }

        if (steps <= 0) {
            (moreWork, mapping)
        } else {
            val nextResults = moreWork.map { doWork(_, steps - 1) }
            val nextWork = nextResults.map{ _._1 }.flatten
            val nextMapping =  nextResults.map{ _._2 }.flatten
            (nextWork, mapping ++ nextMapping)
        }
    }

}
