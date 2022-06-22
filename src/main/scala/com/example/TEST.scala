package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.example.Server.{AddPublishers, AddSubscribers, BindAddress, DeletePublishers, DeleteSubscribers, GetPublishers, GetSubscribers}

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

  var InternalPublishers: List[Publisher] = List.empty[Publisher]
  var InternalSubscribers: List[Subscriber] = List.empty[Subscriber]

  val prefix = "/"

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
      if (!x.startsWith(prefix)) {
        sender ! Write(ByteString(s"Type " + prefix + "help for a list of commands."))
      }
      else if (x.startsWith("/help")) {
        sender ! Write(ByteString(s"\nAvailable Commands: " +
          " \n" + prefix + "publish" +
          " \n" + prefix + "subscribe <topic>" +
          " \n" + prefix + "help" +
          " \n" + prefix + "quit"))
      }
      else if (x.startsWith("/quit")) {
        println(s"Connection Closed with ${sender()}")
        context stop self
      }
    case _: ConnectionClosed => println(s"Connection Closed with ${sender()}")
      context stop self
  }

  def handlePublisher(publisher: Publisher): Unit = {
    context.parent ! AddPublishers(publisher)
    implicit val timeout: Timeout = Timeout(5 seconds)
    val future = context.parent ? GetPublishers
    InternalPublishers = Await.result(future, timeout.duration).asInstanceOf[List[Publisher]]
    println("After future " + InternalPublishers)
    //to add sending to subscribers
  }

  def handleSubscriber(sender: ActorRef, x: ByteString): Unit = {
    val cmd = x.utf8String.split(" ", 2)

    if (x.length < 9) {
      sender ! Write(ByteString(s"\n That command requires an argument."))
    }
    if (!getAvailableTopics.contains(cmd.tail)) {
      sender ! Write(ByteString(s"\n That is not an available topic to subscribe to." +
        "\nType " + prefix + "alltopics to see a list of available topics to subscribe to."))
    }
    //implement already subscribed
  }

  def acceptNewPublisher(sender: ActorRef, x: ByteString): Unit = {
    var topic = x.utf8String
    topic = topic.substring(topic.lastIndexOf(" ") + 1)
    sender ! Write(ByteString(s"All further messages will now be published to the \"" + topic + "\" topic." +
      "\nType /start to begin publishing." +
      "\nType /quit to exit."))
    handlePublisher(new Publisher(sender, topic))
  }

  def getAvailableTopics: List[String] = {
    val topics: List[String] = List()
    topics
  }

  def print(x: ByteString): Unit = {
    println(x.decodeString(Charset.defaultCharset()))
  }
}
object Handler {
  //final case class AddPublisher(publisher: Publisher)
 // case class SetProducers(x: List[Publisher])
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


