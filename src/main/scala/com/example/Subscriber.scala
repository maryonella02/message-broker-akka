package com.example

import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

class Subscriber(sender: ActorRef) {
  private val topics: ListBuffer[String] = ListBuffer[String]()


  def getSubscribedTopics: List[String] = topics.toList

  def getActorRef: ActorRef = sender

  def subscribeToTopic(topic: String): Unit = {
    topics :+ topic
  }

  def unsubscribeFromTopic(topic: String): Unit = {
    topics -= topic
  }

  def isSubscribedToTopic(topic: String): Boolean = {
    topics.contains(topic)
  }

}
