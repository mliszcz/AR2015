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

    def receive = {

        case WorkRequestMsg() =>
            println(s"workRequest $sender")
            work match {
                case workToAssign :: restOfWork =>
                    sender ! WorkAssignmentMsg(workToAssign)
                    assignedTaskCount += 1
                    work = restOfWork
                case Nil =>
                    waitingWorkers = sender :: waitingWorkers
            }

        case WorkDoneMsg((newWork, mapping)) =>

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

            if (work.isEmpty && assignedTaskCount == 0) {
                println("done!!!!")
                pprint.pprintln(solutions)
//                println(solutions)
                import scala.concurrent.duration._
                Await.result(context.system.terminate(), 10 seconds)
            }
    }
  }
