package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import akka.util.ByteString
import com.example.Server.BindAddress

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


  def receive: Receive = {
    case Received(x) => print(x)

      if (x.startsWith("/publish")) {
        acceptNewPublisher(sender())
      }
      else if (x.startsWith("CONNECTION-SUBSCRIBER")) {
        acceptNewSubscriber()
      }
      else if(x.startsWith("topic")){
        val topic = x.utf8String
        sender() ! Write(ByteString(s"All further messages will now be published to the \"" + topic + "\" topic." +
          "\nType /quit to exit."))
      }
    case x: ConnectionClosed => println(s"Connection Closed with ${sender()}")
      context stop self
  }

  def acceptNewPublisher(sender: ActorRef): Unit = {
    sender ! Write(ByteString(s"\nEnter the name of the topic that you would like to create and publish to: "))
  }

  def acceptNewSubscriber(): Unit = {

  }

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


