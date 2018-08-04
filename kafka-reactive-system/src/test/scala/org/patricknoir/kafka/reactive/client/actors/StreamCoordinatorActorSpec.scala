package org.patricknoir.kafka.reactive.client.actors

import akka.Done
import akka.actor.{ Actor, ActorSystem, Props }
import akka.testkit.{ TestKit, TestProbe }
import akka.util.Timeout
import io.circe.generic.auto._
import org.patricknoir.kafka.reactive.client.actors.protocol.{ ResponseInfo, SendMessageComplete, StreamRequest, StreamRequestWithSender }
import org.patricknoir.kafka.reactive.common._
import org.patricknoir.kafka.reactive.common.serializer._
import org.specs2.SpecificationLike

import scala.concurrent.duration._

/**
 * Created by patrick on 13/07/2016.
 */
class StreamCoordinatorActorSpec extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

  val echoActor = system.actorOf(Props(new KafkaEchoMockActor), "echo")
  val coordinatorActor = system.actorOf(StreamCoordinatorActor.props, "request-coord")

  def is =
    s2"""

    simple string request    $simpleStringRequest
    simple object request    $simpleObjectRequest
    one way request          $fireAndForgetRequest

  """

  def simpleStringRequest = {

    implicit val timeout = Timeout(10 seconds)
    val destination = Destination("kafka", "destinationTopic", "echoService")
    val probe = TestProbe()

    coordinatorActor.tell(
      msg = StreamRequestWithSender(
        origin = probe.ref,
        request = StreamRequest(
          destination = destination,
          payload = new String(serialize("simple message".getBytes)),
          timeout = timeout,
          responseInfo = Option(ResponseInfo(
            replyTo = "replyTopic",
            deserializer = implicitly[ReactiveDeserializer[String]])),
          Map.empty[String, String])),
      sender = echoActor)

    val expectedMsg = "simple message"
    probe.expectMsg(timeout.duration, expectedMsg) must be_==(expectedMsg)
  }

  def simpleObjectRequest = {

    case class Car(model: String, constructor: String, year: Int)

    val car = Car("Carrera S 997", "Porsche", 2008)

    implicit val timeout = Timeout(10 seconds)
    val destination = Destination("kafka", "destinationTopic", "echoService")
    val probe = TestProbe()

    coordinatorActor.tell(
      msg = StreamRequestWithSender(
        origin = probe.ref,
        request = StreamRequest(
          destination = destination,
          payload = new String(serialize(car)),
          timeout = timeout,
          responseInfo = Option(ResponseInfo(
            replyTo = "replyTopic",
            deserializer = implicitly[ReactiveDeserializer[Car]])),
          headers = Map.empty[String, String])),
      sender = echoActor)

    val expectedMsg = car
    probe.expectMsg(timeout.duration, expectedMsg) must be_==(car)
  }

  def fireAndForgetRequest = {
    implicit val timeout = Timeout(10 seconds)
    val destination = Destination("kafka", "destinationTopic", "echoService")
    val probe = TestProbe()

    coordinatorActor.tell(
      msg = StreamRequestWithSender(
        origin = probe.ref,
        request = StreamRequest(
          destination = destination,
          payload = new String(serialize("simple message".getBytes)),
          timeout = timeout,
          responseInfo = None,
          headers = Map.empty[String, String])),
      sender = echoActor)

    val expectedMsg = Done
    probe.expectMsg(timeout.duration, expectedMsg) must be_==(expectedMsg)
  }

}

class KafkaEchoMockActor extends Actor {
  def receive = {
    case KafkaRequestEnvelope(correlationId, _, _, replyTo, _) if replyTo.isEmpty =>
      sender ! SendMessageComplete(correlationId)
    case KafkaRequestEnvelope(correlationId, destination, payload, replyTo, _) if replyTo.nonEmpty =>
      sender ! KafkaResponseEnvelope(correlationId, replyTo, payload, KafkaResponseStatusCode.Success)
  }
}