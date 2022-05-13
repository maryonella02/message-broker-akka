//package com.example
//import akka.actor.{ Actor, Props }
//import akka.io.{ IO, Tcp }
//import java.net.InetSocketAddress
////#imports
//
//class Server extends Actor {
//
//  import Tcp._
//  import context.system
//
//  IO(Tcp) ! Bind(self, new InetSocketAddress("localhost", 0))
//
//  def receive = {
//    case b @ Bound(localAddress) =>
//      //#do-some-logging-or-setup
//      context.parent ! b
//    //#do-some-logging-or-setup
//
//    case CommandFailed(_: Bind) => context.stop(self)
//
//    case c @ Connected(remote, local) =>
//      //#server
//      context.parent ! c
//      //#server
//      val handler = context.actorOf(Props[SimplisticHandler]())
//      val connection = sender()
//      connection ! Register(handler)
//  }
//
//}
