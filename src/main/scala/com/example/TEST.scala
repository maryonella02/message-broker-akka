package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.example.Server.{AddPublishers, AddSubscribers, BindAddress, DeletePublishers, DeleteSubscribers, GetPublishers, GetSubscribers}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps


class Server extends Actor {
  implicit val system: ActorSystem = context.system
  val prefix = "/"

  var Publishers: List[Publisher] = List.empty[Publisher]
  var Subscribers: List[Subscriber] = List.empty[Subscriber]

  def receive: Receive = {
    case BindAddress(a, p) =>
      IO(Tcp) ! Bind(self, localAddress = new InetSocketAddress(a, p))
      println(s"server running on ip $a with port $p")
    case Connected(_, _) =>
      println(sender())
      sender() ! Tcp.Register(context.actorOf(Props[Handler]))
      sender() ! Write(ByteString(s"Type " + prefix + "help for a list of commands." +
        "\nType " + prefix + "quit to exit."))
    case Bound(a) => println(a.getAddress)
    case AddPublishers(a) =>
      Publishers = Publishers :+ a
    case AddSubscribers(a) =>
      Subscribers = Subscribers :+ a
    case DeletePublishers(a) =>
      removePublisher(a, Publishers)
    case DeleteSubscribers(a) =>
      removeSubscriber(a, Subscribers)
    case GetPublishers =>
      sender() ! Publishers
    case GetSubscribers =>
      sender() ! Subscribers
    case _ =>
      println("unrecognized command")
  }

  def removePublisher(num: Publisher, list: List[Publisher]): List[Publisher] = list diff List(num)
  def removeSubscriber(num: Subscriber, list: List[Subscriber]): List[Subscriber] = list diff List(num)
}

class Handler extends Actor {
  val prefix = "/"
  var Topic = ""

  def receive: Receive = {
    case Received(x) => print(x)
      if (x.startsWith("/publish")) {
        sender ! Write(ByteString(s"\n Type 'topic ' and the topic that you would like publish to: "))
      }
      else if (x.startsWith("/subscribe")) {
        handleSubscriber(sender(), x)
      }
      else if (x.startsWith("topic")) {
        acceptNewPublisher(sender, x)
      }
      else if (!x.startsWith(prefix) && !x.startsWith("{")) {
        sender ! Write(ByteString(s"Type " + prefix + "help for a list of commands."))
      }
      else if (x.startsWith("/help")) {
        sender ! Write(ByteString(s"\nAvailable Commands: " +
          " \n" + prefix + "publish" +
          " \n" + prefix + "subscribe <topic>" +
          " \n" + prefix + "help" +
          " \n" + prefix + "quit"))
      }
      else if (x.startsWith("{")) {
        RedirectMessage(x)
      }
      else if (x.startsWith("/quit")) {
        println(s"Connection Closed with ${sender()}")
      }
    case _: ConnectionClosed =>
      println(s"Connection Closed with ${sender()}")
      context stop self
  }

  def RedirectMessage(x: ByteString): Unit = {
    for (sub <- getSubscribers) {
      println(sub.getSubscribedTopics)
      if (sub.isSubscribedToTopic(Topic)) {
        sub.getActorRef ! Write(ByteString(x))
      }
    }
  }
  def handlePublisher(publisher: Publisher): Unit = {
    context.parent ! AddPublishers(publisher)
    for (sub <- getSubscribers) {
      if (sub.isSubscribedToTopic(publisher.getTopic)) {
        sub.getActorRef ! Write(ByteString(s"\n Shit is getting real" + sub.getSubscribedTopics))
      }
    }
  }

  def getPublishers: List[Publisher] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    val future = context.parent ? GetPublishers
    Await.result(future, timeout.duration).asInstanceOf[List[Publisher]]
  }
  def getSubscribers: List[Subscriber] = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    val future = context.parent ? GetSubscribers
    Await.result(future, timeout.duration).asInstanceOf[List[Subscriber]]
  }

  def handleSubscriber(sender: ActorRef, x: ByteString): Unit = {
    val subscriber = new Subscriber(sender)
    val cmd = x.utf8String.split(" ", 2)

    if (x.utf8String.length < 11) {
      sender ! Write(ByteString(s"\n That command requires an argument."))
    }
    else if (!getAvailableTopics.contains(cmd(1))) {
      sender ! Write(ByteString(s"\n That is not an available topic to subscribe to." +
        "\nType " + prefix + "alltopics to see a list of available topics to subscribe to."))
    }
    else if (subscriber.isSubscribedToTopic(cmd(1))) {
      sender ! Write(ByteString(s"\n You are already subscribed to that topic."))
    }
    else {
      subscriber.subscribeToTopic(cmd(1))
      context.parent ! AddSubscribers(subscriber)
      sender ! Write(ByteString(s"\n You have subscribed to topic \"" + cmd(1) + "\""))
    }
  }

  def acceptNewPublisher(sender: ActorRef, x: ByteString): Unit = {
    var topic = x.utf8String
    topic = topic.substring(topic.lastIndexOf(" ") + 1)
    if (getAvailableTopics.contains(topic)) {
      sender ! Write(ByteString(s"That topic already exists. Please choose a different topic: "))
    }
    else {
      Topic = topic
      sender ! Write(ByteString(s"All further messages will now be published to the \"" + topic + "\" topic." +
        "\nType /start to begin publishing." +
        "\nType /quit to exit."))
      handlePublisher(new Publisher(sender, topic))
    }

  }

  def getAvailableTopics: ListBuffer[String] = {
    val topics: ListBuffer[String] = ListBuffer()
    getPublishers.foreach(topics += _.getTopic)
    println(topics.toList)
    topics
  }

  def print(x: ByteString): Unit = {
    println(x.decodeString(Charset.defaultCharset()))
  }
}

object Handler {
}

object Server {
  case class BindAddress(addr: String, port: Int)

  case class AddPublishers(a: Publisher)

  case class AddSubscribers(a: Subscriber)

  case class DeletePublishers(a: Publisher)

  case class DeleteSubscribers(a: Subscriber)

  case object GetPublishers

  case object GetSubscribers
}

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create("clientServer")
    val serverActorRef = system.actorOf(Props[Server])
    serverActorRef ! BindAddress("127.0.0.1", 9999)
  }
}


