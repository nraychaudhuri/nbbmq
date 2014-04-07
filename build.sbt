name := "nbbmq"

description := "Non blocking bounded message queue"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.1",
  "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"
)

scalaVersion := "2.10.4"

