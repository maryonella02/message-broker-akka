package com.example

import java.net.InetSocketAddress
import java.nio.charset.Charset
import akka.actor._
import akka.io.Tcp._
import akka.io._
import com.example.Server.BindAddress

class Server extends Actor{
  implicit val system = context.system

  override def receive: Receive = {
    case BindAddress(a,p) =>
      IO(Tcp) ! Bind(self,localAddress = new InetSocketAddress(a,p))
      println(s"server running on ip $a with port $p")
    case Connected(_,_)=>
      println(sender())
      sender() ! Tcp.Register(context.actorOf(Props[Handler]))
    case Bound(a) => println(a.getAddress)
  }
}

class Handler extends Actor{
  override def receive: Receive = {
    case Received(x) => println(x.decodeString(Charset.defaultCharset()))
      sender() ! Write(x)
    case x: ConnectionClosed => println("Connection Closed bro")
  }
}

object Server{
  case class BindAddress(addr: String, port: Int)
}

object Main{
  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create("clientServer")
    val serverActorRef = system.actorOf(Props[Server])
    serverActorRef ! BindAddress("127.0.0.1",9999)
  }
}


