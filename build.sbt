name := "Matching"

version := "0.1"

scalaVersion := "2.12.7"

// akka
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.12"

// logging
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

// for scala_test
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"