package com.example

import akka.actor.typed.Behavior
import akka.actor.typed.PostStop
import akka.actor.typed.Signal
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
object MainSupervisor{
  def apply():Behavior[Nothing] =
    Behaviors.setup[Nothing](context => new MainSupervisor(context))
}

class MainSupervisor(context: ActorContext[Nothing]) extends AbstractBehavior[Nothing](context) {
  context.log.info("Message Broker Application started")

  override def onMessage(msg: Nothing): Behavior[Nothing] = {
    // No need to handle any messages
    Behaviors.unhandled
  }

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop =>
      context.log.info("Message Broker Application stopped")
      this
  }
}

