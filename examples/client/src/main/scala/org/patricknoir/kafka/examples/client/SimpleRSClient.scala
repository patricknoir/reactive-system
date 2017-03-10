package org.patricknoir.kafka.examples.client

import akka.actor.ActorSystem
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.io.StdIn

/**
 * Created by patrick on 09/08/2016.
 */
object SimpleRSClient extends App {

  implicit val system = ActorSystem("ReactiveClient")
  implicit val timeout = Timeout(5 seconds)

  import system.dispatcher

  val client = new KafkaReactiveClient(KafkaRClientSettings.default)

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
