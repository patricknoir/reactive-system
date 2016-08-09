package org.patricknoir.kafka.examples.client

import akka.actor.ActorSystem
import akka.util.Timeout
import cats.data.Xor
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

/**
 * Created by patrick on 09/08/2016.
 */
class SimpleRSClient {

  implicit val system = ActorSystem("ReactiveClient")
  implicit val timeout = Timeout(10 seconds)

  import system.dispatcher

  val client = new KafkaReactiveClient(KafkaRClientSettings.default)

  val response: Future[Error Xor String] = client.request[String, String]("simple/echo", "hello world!")

  response.onComplete(println)

  Await.ready(response, Duration.Inf)
}
