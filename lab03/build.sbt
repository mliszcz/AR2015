name := "lab03"

organization := "pl.edu.agh.ar"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.7"

sbtVersion := "0.13.9"

libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
    "ch.qos.logback" % "logback-classic" % "1.1.3"
)

libraryDependencies ++= Seq(
    "com.lihaoyi" %% "pprint" % "0.3.6"
)
