package com.example

import akka.actor.ActorRef

import scala.collection.mutable.ListBuffer

class Subscriber(sender: ActorRef) {
  private var topics: List[String] = List[String]()


  def getSubscribedTopics: List[String] = topics

  def getActorRef: ActorRef = sender

  def subscribeToTopic(topic: String): Unit = {
    topics = topics :+ topic
  }

  def unsubscribeFromTopic(topic: String): Unit = {
    removeTopic(topic, topics)
  }

  def isSubscribedToTopic(topic: String): Boolean = {
    topics.contains(topic)
  }

  def removeTopic(num: String, list: List[String]): List[String] = list diff List(num)

}
