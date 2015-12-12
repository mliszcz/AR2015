package pl.edu.agh.ar.lab03.concurrent

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task
import akka.actor.ActorSystem
import akka.actor.Props

object ConcurrentSolution {

    def buildMappingTree(tasks: List[Task],
                         machines: List[Machine],
                         deadline: Double,
                         unitCost: Double) {

    implicit val system = ActorSystem("TreeSystem")

    val workerCount = 4

    val master = system.actorOf(props = Props(new TreeMaster(tasks,
                                                             machines,
                                                             deadline,
                                                             unitCost,
                                                             workerCount)),
                                name = "master")
    }
}
