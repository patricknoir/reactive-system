package org.patricknoir.kafka.examples.client

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.client2.{ ReactiveClientStream, StreamClientConfig }

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.io.StdIn
import scala.util.{ Failure, Success }

/**
 * Created by patrick on 09/08/2016.
 */
object SimpleRSClient extends App {

  implicit val system = ActorSystem("ReactiveClient")
  implicit val timeout = Timeout(5 seconds)

  import system.dispatcher
  implicit val materializer = ActorMaterializer()
  val client = new ReactiveClientStream(StreamClientConfig.default)
  //  val client = new KafkaReactiveClient(KafkaRClientSettings.default)

  do {
    val response: Future[String] = client.request[String, String]("kafka:simple/echo", "hello world!")

    response.onComplete { r =>
      println(r)
      println("press e + enter to exit, otherwise enter to repeat the request!")
    }

    Await.ready(response, Duration.Inf)
  } while (StdIn.readLine() != "e")

  Await.ready(system.terminate(), Duration.Inf)
  println("program terminated")

}

object SimpleReactiveClientExamples {
  def simpleClient() = {
    //#reactive-client-create-client
    implicit val system = ActorSystem("ReactiveClient")
    implicit val timeout = Timeout(5 seconds)

    import system.dispatcher

    val client = new KafkaReactiveClient(KafkaRClientSettings.default)
    //#reactive-client-create-client
  }

  def simpleClientCallRemoteService() = {
    //#reactive-client-call-get-counter
    implicit val system = ActorSystem("ReactiveClient")
    implicit val timeout = Timeout(5 seconds)

    import system.dispatcher

    val client = new KafkaReactiveClient(KafkaRClientSettings.default)

    val result: Future[Unit] = client.request[Int, Unit]("kafka:simple/incrementCounter", 1)

    result.onComplete {
      case Success(_)   => println("incrementCounter request sent successfully")
      case Failure(err) => println(s"error sending incrementCounter request: ${err.getMessage}")
    }

    Await.ready(result, Duration.Inf)

    //#reactive-client-call-get-counter
  }
}
