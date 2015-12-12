package pl.edu.agh.ar.lab03.concurrent

import pl.edu.agh.ar.lab03.Model.Machine
import pl.edu.agh.ar.lab03.Model.Mapping
import pl.edu.agh.ar.lab03.Model.MappingTree
import pl.edu.agh.ar.lab03.Model.Task

package object Messages {

    /** Work: assign task to the machine */
    case class WorkDescription(
        val task: Task,
        val machine: Machine,
        val parentMapping: Mapping,
        val restTasks: List[Task],
        val usedMachines: List[Machine],
        val freeMachines: List[Machine])

    case class WorkRequestMsg()

    case class WorkAssignmentMsg(val work: WorkDescription)

    type WorkResult = Tuple2[Seq[WorkDescription], Seq[Mapping]]

    case class WorkDoneMsg(val newWorkAndSolutions: WorkResult)

    case class WorkDoneAckMsg()

    case class ReportRequestMsg()

    case class ReportResponseMsg(val totalWork: Int,
                                 val discardedWork: Int)
}
