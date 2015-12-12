package pl.edu.agh.ar.lab03.concurrent

import akka.actor.Actor
import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task
import pl.edu.agh.ar.lab03.concurrent.Messages._
import akka.actor.ActorSystem
import akka.actor.Props
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import scala.concurrent.Await
import scala.annotation.tailrec

class TreeMaster(
    val tasks: List[Task],
    val machines: List[Machine],
    val deadline: Double,
    val unitCost: Double,
    workerCount: Int)
    (implicit system: ActorSystem) extends Actor {

    val workers = (1 to workerCount) map { i =>
        system.actorOf(props = Props(new TreeWorker(self,
                                                    tasks,
                                                    machines,
                                                    deadline,
                                                    unitCost)),
                       name = s"worker_${i-1}")
    }

    var work: List[WorkDescription] = {
        val tasksHead :: tasksTail = tasks
        val machinesHead :: machinesTail = machines
        val wd = WorkDescription(tasksHead,
                                 machinesHead,
                                 Map(),
                                 tasksTail,
                                 List(machinesHead),
                                 machinesTail)
        List(wd)
    }

    var waitingWorkers = List.empty[ActorRef]
    var assignedTaskCount = 0

    var solutions = List.empty[Mapping]

    var reports = List.empty[ReportResponseMsg]

    val startTime = System.currentTimeMillis

    var wdRecv = true

    def receive = {

        case WorkRequestMsg() =>
            work match {
                case workToAssign :: restOfWork =>
                    sender ! WorkAssignmentMsg(workToAssign)
                    assignedTaskCount += 1
                    work = restOfWork
                case Nil =>
                    waitingWorkers = sender :: waitingWorkers
            }

        case WorkDoneMsg((newWork, mapping)) =>

//            if (wdRecv) {
//                println("WORK DONE")
//                println((System.currentTimeMillis - startTime)/1000.0)
//                wdRecv = false
//            }



            work = newWork.toList ++ work
            solutions = mapping.toList ++ solutions
            assignedTaskCount -= 1

            sender ! WorkDoneAckMsg()

            val workToAssign = work.take(waitingWorkers.size)

            workToAssign.zip(waitingWorkers).foreach {
                case (wta, worker) => worker ! WorkAssignmentMsg(wta)
            }

            waitingWorkers = waitingWorkers.drop(workToAssign.size)
            assignedTaskCount += workToAssign.size

            // all work done
            if (work.isEmpty && assignedTaskCount == 0) {
                workers.foreach { worker =>
                    worker ! ReportRequestMsg()
                }
            }

        case report @ ReportResponseMsg(totalWork, discardedWork) =>

            reports = report :: reports

            println(s"WORKER REPORT: " +
                    s"totalWork=$totalWork discardedWork=$discardedWork")

            // last report received
            if (reports.size == workers.size) {

                val deltaTime = System.currentTimeMillis - startTime

                def factorial(n: BigInt): BigInt = {
                    @tailrec
                    def factorial2(n: BigInt, result: BigInt): BigInt =
                        if (n==0) result else factorial2(n-1, n*result)
                    factorial2(n, 1)
                }

                val machinesSize = BigInt(machines.size)

                def possibleAssignments(taskCount: Int):BigInt = {
                    val usedMachines =
                        Math.min(taskCount, machinesSize.intValue)
                    val d = (taskCount - usedMachines)
                    factorial(usedMachines) * machinesSize.pow(d)
                }

                val treeSize = (1 to tasks.size).map(possibleAssignments).sum

                val visitedNodes = reports.map { _.totalWork } sum

                println(s"treeSize=$treeSize")
                println(s"visitedNodes=$visitedNodes")

                println(s"possibleSolutions=${possibleAssignments(tasks.size)}")
                println(s"matchingSolutions=${solutions.size}")

                println(s"deltaTime=${deltaTime/1000.0}")

//                pprint.pprintln(solutions)

                import scala.concurrent.duration._
                Await.result(context.system.terminate(), 10 seconds)
            }
    }
  }
