package org.patricknoir.kafka.reactive.client.actors

import java.util.concurrent.{ ArrayBlockingQueue, BlockingQueue }

import akka.actor.{ Props, Actor, ActorSystem }
import akka.testkit.TestKit
import akka.util.Timeout
import cats.data.Xor
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseStatusCode, KafkaResponseEnvelope }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.KafkaRequest
import org.patricknoir.kafka.reactive.common.ReactiveDeserializer
import org.specs2.SpecificationLike

import scala.concurrent.{ Await, Future }
import akka.pattern.ask
import scala.concurrent.duration._
import io.circe.syntax._

/**
 * Created by patrick on 14/07/2016.
 */
class KafkaRClientActorSpec extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

  def is = s2"""

    client test with simple object    $simpleObjectRequest

  """

  val queue = new ArrayBlockingQueue[KafkaRequestEnvelope](1024)

  val client = system.actorOf(
    KafkaRClientActor.props(
      Props(new MockKafkaProducerActor(queue)),
      Props(new MockKafkaConsumerActor(queue))
    ),
    "client"
  )

  def simpleObjectRequest = {
    implicit val timeout = Timeout(10 seconds)

    import io.circe.generic.auto._
    case class Car(model: String, constructor: String, year: Int)

    val porsche997 = Car("Carrera S 997", "Porsche", 2008)

    val fResp = (client ? KafkaRequest("kafka:destinationTopic/echoService", porsche997.asJson.noSpaces, timeout, "replyTopic", implicitly[ReactiveDeserializer[Car]])).mapTo[Error Xor Car]

    val Xor.Right(result) = Await.result(fResp, Duration.Inf)

    result must be_==(porsche997)
  }

}

class MockKafkaProducerActor(queue: BlockingQueue[KafkaRequestEnvelope]) extends Actor {
  def receive = {
    case req: KafkaRequestEnvelope => queue.add(req); () //return Unit
  }
}

class MockKafkaConsumerActor(queue: BlockingQueue[KafkaRequestEnvelope]) extends Actor {

  import context.dispatcher
  var terminate = false

  val future = Future {
    while (!terminate) {
      val request = queue.take()
      context.actorSelection(request.correlationId) ! KafkaResponseEnvelope(request.correlationId, request.replyTo, request.payload, KafkaResponseStatusCode.Success)
    }
  }

  def receive = Actor.emptyBehavior

  override def postStop(): Unit = {
    terminate = true
  }
}
