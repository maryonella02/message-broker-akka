package com.example

import akka.actor.ActorRef

class Publisher(sender: ActorRef, topic: String) {
  def getTopic: String = topic
  def getActorRef: ActorRef = sender
}
