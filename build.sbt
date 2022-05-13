name := "message-broker"

version := "0.1"

scalaVersion := "2.13.8"

val AkkaVersion = "2.6.19"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % AkkaVersion
