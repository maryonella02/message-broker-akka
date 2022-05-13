package com.example
import akka.actor.typed.ActorSystem
import akka.actor.{ Actor}

object MessageBrokerApp {

  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    ActorSystem[Nothing](MainSupervisor(), "message-broker")
  }
}

class DemoActor extends Actor {
  //#manager
  import akka.io.{ IO, Tcp }
  import context.system // implicitly used by IO(Tcp)

  val manager = IO(Tcp)
  //#manager

  def receive = Actor.emptyBehavior
}