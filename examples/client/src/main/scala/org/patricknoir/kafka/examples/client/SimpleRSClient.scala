package org.patricknoir.kafka.examples.client

import akka.actor.ActorSystem
import akka.util.Timeout
import cats.data.Xor
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
  implicit val timeout = Timeout(30 seconds)

  import system.dispatcher

  val client = new KafkaReactiveClient(KafkaRClientSettings.default)

  var repeat = true

  while (repeat) {
    val response: Future[Error Xor String] = client.request[String, String]("simple/echo", "hello world!")

    response.onComplete(println)

    Await.ready(response, Duration.Inf)
    println("press e + enter to exit, otherwise enter to repeat the request!")
    if (StdIn.readLine() == "e")
      repeat = false
  }

  Await.ready(system.terminate(), Duration.Inf)
  println("program terminated")

}
