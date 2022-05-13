package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import akka.util.ByteString
import com.example.Server.BindAddress

class Server extends Actor {
  implicit val system = context.system

  def receive = {
    case BindAddress(a, p) =>
      IO(Tcp) ! Bind(self, localAddress = new InetSocketAddress(a, p))
      println(s"server running on ip $a with port $p")
    case Connected(_, _) =>
      println(sender())
      sender() ! Tcp.Register(context.actorOf(Props[Handler]))
    case Bound(a) => println(a.getAddress)
  }
}

class Handler extends Actor {
  def receive = {
    case Received(x) => println(x.decodeString(Charset.defaultCharset()))
      sender() ! Write(x)
      if (x.startsWith("CONNECTION-PUBLISHER")) {
        acceptNewPublisher(sender())
      }
      else if (x.startsWith("CONNECTION-SUBSCRIBER")) {
        acceptNewSubscriber
      }
    case x: ConnectionClosed => println(s"Connection Closed with ${sender()}")
  }

  def acceptNewPublisher(sender: ActorRef) = {
    sender ! Write(ByteString(s"Enter the name of the topic that you would like to create and publish to: "))
  }

  def acceptNewSubscriber = {

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


