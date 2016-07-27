package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ Props, Actor, ActorSystem }
import akka.testkit.TestKit
import akka.util.Timeout
import cats.data.Xor
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseStatusCode, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import org.specs2.SpecificationLike
import akka.pattern.ask
import scala.concurrent.Await
import scala.concurrent.duration._
import org.patricknoir.kafka.reactive.common.{ ReactiveDeserializer }
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

    val fResp = (requestActor ? KafkaRequest("kafka:destinationTopic/echoService", new String(serialize("simple message".getBytes)), timeout, "replyTopic", implicitly[ReactiveDeserializer[String]])).mapTo[Error Xor String]

    val Xor.Right(result) = Await.result(fResp, Duration.Inf)

    result must be_==("simple message")
  }

  def simpleObjectRequest = {

    case class Car(model: String, constructor: String, year: Int)

    import io.circe.generic.auto._
    val car = Car("Carrera S 997", "Porsche", 2008)

    implicit val timeout = Timeout(10 seconds)
    val requestActor = system.actorOf(KafkaRRequestActor.props(echoActor), "request-2")

    val fResp = (requestActor ? KafkaRequest("kafka:destinationTopic/echoService", new String(serialize(car)), timeout, "replyTopic", implicitly[ReactiveDeserializer[Car]])).mapTo[Error Xor Car]

    val Xor.Right(carResult) = Await.result(fResp, Duration.Inf)

    carResult must be_==(car)

  }

}

class KafkaEchoMockActor extends Actor {
  def receive = {
    case KafkaRequestEnvelope(correlationId, destination, payload, replyTo) =>
      sender ! KafkaResponseEnvelope(correlationId, replyTo, payload, KafkaResponseStatusCode.Success)
  }
}