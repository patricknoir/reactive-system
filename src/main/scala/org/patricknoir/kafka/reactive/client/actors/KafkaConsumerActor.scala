package org.patricknoir.kafka.reactive.client.actors

import akka.actor.{ Props, Actor, ActorLogging }
import io.circe.Decoder
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope

import scala.collection.JavaConversions._
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import io.circe.parser._

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaConsumerActor(consumerSettings: Map[String, String], inboundQueue: String, pollTimeout: FiniteDuration) extends Actor with ActorLogging {

  import KafkaResponseEnvelope._

  import context.dispatcher

  val consumer = new KafkaConsumer[String, String](consumerSettings)
  consumer.subscribe(List(inboundQueue))

  val loop = Future {
    consumer.poll(pollTimeout.toMillis).foreach { record =>
      val result = decode[KafkaResponseEnvelope](record.value)(respEnvelopeDecoder)
      result.foreach { envelope =>
        context.actorSelection(envelope.correlationId) ! envelope
      }
    }
  }

  def receive = Actor.emptyBehavior

  override def postStop() = {
    consumer.close()
  }

}

object KafkaConsumerActor {
  object KafkaResponseStatusCode {
    val Success = 200
    val NotFound = 404
    val BadRequest = 300
    val InternalServerError = 500
  }
  case class KafkaResponseEnvelope(correlationId: String, response: String, statusCode: Int)
  object KafkaResponseEnvelope {
    implicit val respEnvelopeDecoder = Decoder.instance[KafkaResponseEnvelope] { c =>
      for {
        correlationId <- c.downField("correlationId").as[String]
        response <- c.downField("response").as[String]
        statusCode <- c.downField("status").as[Int]
      } yield KafkaResponseEnvelope(correlationId, response, statusCode)
    }
  }
  def props(consumerSettings: Map[String, String], inboundQueue: String, pollTimeout: FiniteDuration) =
    Props(new KafkaConsumerActor(consumerSettings, inboundQueue, pollTimeout))
}
