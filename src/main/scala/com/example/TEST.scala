package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import akka.util.ByteString
import com.example.Server.BindAddress

import scala.collection.mutable.ListBuffer

class Server extends Actor {
  implicit val system: ActorSystem = context.system
  val prefix = "/"

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
  }
}

class Handler extends Actor {

  var Publishers: ListBuffer[Publisher] = ListBuffer()
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
    case x: ConnectionClosed => println(s"Connection Closed with ${sender()}")

      context stop self
  }

  def handlePublisher(publisher: Publisher): Unit = {
    Publishers :+ publisher
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

  //  def acceptNewSubscriber(): Unit = {
  //
  //  }

  def print(x: ByteString): Unit = {
    println(x.decodeString(Charset.defaultCharset()))
  }
}

object Server {
  case class BindAddress(addr: String, port: Int)
}

object Main {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create("clientServer")
    val serverActorRef = system.actorOf(Props[Server])
    serverActorRef ! BindAddress("127.0.0.1", 9999)
  }
}


