package pl.edu.agh.ar.lab03

package object Model {

    case class Task(val id: Int, val duration: Double)

    case class Machine(val id: Int)

    case class Tree[T](val node: T, val children: Seq[Tree[T]])

    type Mapping = Map[Task, Machine]

    type MappingTree = Tree[Mapping]
}
