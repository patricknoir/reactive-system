package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.TestKit
import akka.util.Timeout
import org.patricknoir.kafka.reactive.common.{ KafkaResponseEnvelope, KafkaResponseStatusCode }
import org.patricknoir.kafka.reactive.common.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.{ Destination, KafkaRequest }
import org.specs2.SpecificationLike
import akka.pattern.ask
import org.patricknoir.kafka.reactive.client.actors.KafkaRRequestActor.KafkaProducerRequest

import scala.concurrent.Await
import scala.concurrent.duration._
import org.patricknoir.kafka.reactive.common.ReactiveDeserializer
import org.patricknoir.kafka.reactive.common.serializer._

/**
 * Created by patrick on 13/07/2016.
 */
class KafkaRRequestActorSpec extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

  def is = s2"""

    simple string request    $simpleStringRequest
    simple object request    $simpleObjectRequest

  """

  val echoActor = system.actorOf(Props(new KafkaEchoMockActor), "echo")

  def simpleStringRequest = {

    implicit val timeout = Timeout(10 seconds)
    val requestActor = system.actorOf(KafkaRRequestActor.props(echoActor), "request-1")
    val destination = Destination("kafka", "destinationTopic", "echoService")

    val fResp = (requestActor ? KafkaRequest(destination, new String(serialize("simple message".getBytes)), timeout, "replyTopic", implicitly[ReactiveDeserializer[String]])).mapTo[String]

    val result = Await.result(fResp, Duration.Inf)

    result must be_==("simple message")
  }

  def simpleObjectRequest = {

    case class Car(model: String, constructor: String, year: Int)

    import io.circe.generic.auto._
    val car = Car("Carrera S 997", "Porsche", 2008)

    implicit val timeout = Timeout(10 seconds)
    val requestActor = system.actorOf(KafkaRRequestActor.props(echoActor), "request-2")

    val destination = Destination("kafka", "destinationTopic", "echoService")

    val fResp = (requestActor ? KafkaRequest(destination, new String(serialize(car)), timeout, "replyTopic", implicitly[ReactiveDeserializer[Car]])).mapTo[Car]

    val carResult = Await.result(fResp, Duration.Inf)

    carResult must be_==(car)

  }

}

class KafkaEchoMockActor extends Actor {
  def receive = {
    case KafkaProducerRequest(KafkaRequestEnvelope(correlationId, destination, payload, replyTo), _) =>
      sender ! KafkaResponseEnvelope(correlationId, replyTo, payload, KafkaResponseStatusCode.Success)
  }
}