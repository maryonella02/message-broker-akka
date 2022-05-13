package com.example
import akka.actor.typed.ActorSystem

object MessageBrokerApp {

  def main(args: Array[String]): Unit = {
    // Create ActorSystem and top level supervisor
    ActorSystem[Nothing](MainSupervisor(), "message-broker")
  }
}
