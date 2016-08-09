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

  var running = true

  val consumer = new KafkaConsumer[String, String](consumerSettings)
  consumer.subscribe(List(inboundQueue))

  val loop = Future {
    while (running) {
      consumer.poll(pollTimeout.toMillis).foreach { record =>
        val result = decode[KafkaResponseEnvelope](record.value)(respEnvelopeDecoder)
        result.foreach { envelope =>
          context.actorSelection(envelope.correlationId) ! envelope
        }
      }
    }
  }

  loop.onComplete(result => println(log.info("Kafka Consumer Terminated")))

  def receive = Actor.emptyBehavior

  override def postStop() = {
    running = false
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
  case class KafkaResponseEnvelope(correlationId: String, replyTo: String, response: String, statusCode: Int)
  object KafkaResponseEnvelope {
    implicit val respEnvelopeDecoder = Decoder.instance[KafkaResponseEnvelope] { c =>
      for {
        correlationId <- c.downField("correlationId").as[String]
        replyTo <- c.downField("replyTo").as[String]
        response <- c.downField("response").as[String]
        statusCode <- c.downField("statusCode").as[Int]
      } yield KafkaResponseEnvelope(correlationId, replyTo, response, statusCode)
    }
  }
  def props(consumerSettings: Map[String, String], inboundQueue: String, pollTimeout: FiniteDuration) =
    Props(new KafkaConsumerActor(consumerSettings, inboundQueue, pollTimeout))
}
