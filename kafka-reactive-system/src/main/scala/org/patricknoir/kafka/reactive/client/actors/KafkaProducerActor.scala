package org.patricknoir.kafka.reactive.client.actors

import java.util.concurrent.TimeUnit

import akka.actor.{ Props, ActorLogging, Actor }
import akka.event.LoggingReceive
import io.circe.Decoder
import org.apache.kafka.clients.producer.{ ProducerRecord, KafkaProducer }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import io.circe.generic.auto._
import io.circe.syntax._
import org.patricknoir.kafka.reactive.ex.ProducerException

import scala.collection.JavaConversions._
import scala.util.Try

/**
 * Created by patrick on 12/07/2016.
 */
class KafkaProducerActor(producerSettings: Map[String, String]) extends Actor with ActorLogging {

  val producer = new KafkaProducer[String, String](producerSettings)

  def receive = LoggingReceive {
    case envelope: KafkaRequestEnvelope =>
      val result = extractDestinationTopic(envelope.destination).map { destTopic =>
        val record = new ProducerRecord[String, String](destTopic, envelope.asJson.noSpaces)

        //TODO: handle the error by throwing a ProducerException to be handled by the suprvisor.
        //      I need to block: shall I do in a new thread to do not kill the actor performance?
        //      Risk to lose requests if I wrap in to a future and then I block! Design decision to be made.
        try {
          producer.send(record).get(100, TimeUnit.MILLISECONDS)
        } catch {
          case err: Throwable => throw new ProducerException(err)
        }
      }
      if (result.isEmpty) log.warning(s"Discarding message: $envelope, couldn't extract destination topic")
      ()
  }

  private def extractDestinationTopic(destination: String): Option[String] = Try {
    destination.split(":")(1).split("/")(0)
  }.toOption

  override def postStop() = {
    producer.close()
  }

}

object KafkaProducerActor {
  case class KafkaRequestEnvelope(correlationId: String, destination: String, payload: String, replyTo: String)
  object KafkaRequestEnvelope {
    implicit val kafkaRequestEnvelopeDecoder = Decoder.instance[KafkaRequestEnvelope] { c =>
      for {
        correlationId <- c.downField("correlationId").as[String]
        destination <- c.downField("destination").as[String]
        payload <- c.downField("payload").as[String]
        replyTo <- c.downField("replyTo").as[String]
      } yield KafkaRequestEnvelope(correlationId, destination, payload, replyTo)
    }
  }

  def props(producerSettings: Map[String, String]) = Props(new KafkaProducerActor(producerSettings))
}
