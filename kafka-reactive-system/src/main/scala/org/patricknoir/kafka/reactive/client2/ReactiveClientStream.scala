package org.patricknoir.kafka.reactive.client2

import java.util.UUID

import akka.{ Done, NotUsed }
import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props, Status }
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.{ ProducerMessage, ProducerSettings }
import akka.kafka.scaladsl.Producer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl._
import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.ReactiveClient
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.Destination
import org.patricknoir.kafka.reactive.common.{ KafkaRequestEnvelope, KafkaResponseEnvelope, ReactiveDeserializer, ReactiveSerializer }

import scala.concurrent.Future
import scala.reflect.ClassTag
import akka.pattern.ask
import akka.stream.{ Materializer, OverflowStrategy }
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.annotation.tailrec
import protocol.{ StreamRequestWithSender, _ }
import io.circe.generic.auto._

/**
 * Created by patrick on 28/03/2017.
 */
class ReactiveClientStream(config: StreamClientConfig)(implicit system: ActorSystem, materializer: Materializer) extends ReactiveClient {

  import system.dispatcher

  import config._

  val coordinator: ActorRef = system.actorOf(StreamCoordinatorActor.props, "streamCoordinator")

  /** AT MOST ONCE **/

  private val publisher = createStream()

  override def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout, ct: ClassTag[Out]): Future[Out] =
    (publisher ? StreamRequest(destination, new String(implicitly[ReactiveSerializer[In]].serialize(payload)), timeout, Some(ResponseInfo(responseTopic, implicitly[ReactiveDeserializer[Out]])))).mapTo[Out]

  override def send[In: ReactiveSerializer](destination: String, payload: In, confirmSend: Boolean)(implicit timeout: Timeout): Future[Unit] =
    (publisher ? StreamRequest(destination, new String(implicitly[ReactiveSerializer[In]].serialize(payload)), timeout, None)).mapTo[Done].map(_ => ())

  private def createStream(): ActorRef = {
    val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers(bootstrapServers.mkString(","))

    val requestFlow: Flow[StreamRequestWithSender, ProducerMessage.Message[String, String, KafkaRequestEnvelope], NotUsed] = Flow[StreamRequestWithSender].mapAsync(concurrency) { reqWithSender: StreamRequestWithSender =>
      import io.circe.syntax._
      implicit val timeout = reqWithSender.request.timeout
      (coordinator ? reqWithSender).mapTo[KafkaRequestEnvelope].map(envelope => ProducerMessage.Message(new ProducerRecord[String, String](envelope.destination.topic, envelope.asJson.noSpaces), envelope))
    }

    val responseFlow = Flow[KafkaResponseEnvelope].mapAsync(concurrency) { respEnv: KafkaResponseEnvelope =>
      println(s"\n\n\n\n\nREADING RESPONSE: $respEnv\n\n\n\n")
      coordinator ! respEnv
      Future.successful[Any](respEnv)
    }

    val bidiFlow: BidiFlow[StreamRequestWithSender, ProducerMessage.Message[String, String, KafkaRequestEnvelope], KafkaResponseEnvelope, Any, NotUsed] =
      BidiFlow.fromFlows(requestFlow, responseFlow)

    val requestKafkaSink: Sink[ProducerMessage.Message[String, String, KafkaRequestEnvelope], NotUsed] = Producer.flow[String, String, KafkaRequestEnvelope](producerSettings).map { result =>
      val requestEnvelope = result.message.passThrough
      if (requestEnvelope.replyTo.isEmpty) //FIXME: replace replyTo: String with Option[String] and update the server logic too
        coordinator ! SendMessageComplete(requestEnvelope.correlationId)
    }.to(Sink.ignore)

    val kafkaFlow: Flow[ProducerMessage.Message[String, String, KafkaRequestEnvelope], KafkaResponseEnvelope, NotUsed] = Flow.fromSinkAndSource(
      sink = requestKafkaSink,
      source = ReactiveKafkaStreamSource.atMostOnce(responseTopic, bootstrapServers, consumerClientId, consumerGroupId, concurrency)
    )

    val stream: RunnableGraph[ActorRef] = Source.actorPublisher(StreamPublisherActor.props).via(bidiFlow.join(kafkaFlow)).to(Sink.ignore)

    stream.run()
  }

}

class StreamPublisherActor() extends ActorPublisher[StreamRequestWithSender] with ActorLogging {

  override def receive = {
    case req: StreamRequest =>
      onNext(StreamRequestWithSender(sender, req))

  }

}

object StreamPublisherActor {
  lazy val props: Props = Props(new StreamPublisherActor)
}

class StreamCoordinatorActor() extends Actor with ActorLogging {

  def createActorPerRequest(correlationId: String, request: StreamRequestWithSender) = context.actorOf(StreamRequestActor.props(request), correlationId)

  //FIXME: check it doesn't exists already
  @tailrec
  private def generateUUID(): String = {
    val candidate = UUID.randomUUID().toString()
    context.child(candidate) match {
      case None    => candidate
      case Some(_) => generateUUID()
    }
  }

  def receive = {
    case reqWithSender: StreamRequestWithSender =>
      handleRequest(reqWithSender)
    case respEnv @ KafkaResponseEnvelope(correlationId, _, _, _) =>
      forwardToChild(correlationId, respEnv)
    case msgSent @ SendMessageComplete(correlationId) =>
      forwardToChild(correlationId, msgSent)
    case other => log.warning(s"\n\n\nWARNINGGGGG: ${other}\n\n\n")
  }

  def handleRequest(reqWithSender: StreamRequestWithSender) = {
    val correlationId = generateUUID()
    createActorPerRequest(correlationId, reqWithSender)
    reqWithSender.request.destination match {
      case Destination(medium, topic, route) =>
        import reqWithSender.request._
        sender ! KafkaRequestEnvelope(correlationId, Destination(medium, topic, route), payload, responseInfo.map(_.replyTo).getOrElse(""))
    }
    //        Destination.fromString(reqWithSender.request.destination).fold(
    //          err => reqWithSender.origin ! Status.Failure(err), //FIXME: enreach the exception with a domain error
    //          dest => {
    //            import reqWithSender.request._
    //            log.info(s"\n\n\nDESTINATION = $dest\n\n")
    //            sender ! KafkaRequestEnvelope(correlationId, dest, payload, responseInfo.map(_.replyTo).getOrElse(""))
    //          }
    //        )

  }

  def forwardToChild(correlationId: String, msg: Any) = {
    context.child(correlationId).map(_ ! msg).orElse {
      log.warning(s"Unexpected reponse received for: ${correlationId}")
      None
    }
  }
}

object StreamCoordinatorActor {
  def props: Props = Props(new StreamCoordinatorActor())
}

class StreamRequestActor(reqWithSender: StreamRequestWithSender) extends Actor {

  context.setReceiveTimeout(reqWithSender.request.timeout.duration)

  def receive = {
    case responseEvenlope: KafkaResponseEnvelope =>
      val eitherResult = reqWithSender.request.responseInfo.map(_.deserializer.deserialize(responseEvenlope.response.getBytes))
      eitherResult match {
        case Some(Right(response)) => reqWithSender.origin ! response
        case Some(Left(err))       => reqWithSender.origin ! Status.Failure(err)
        case None =>
          reqWithSender.origin ! Status.Failure(new RuntimeException(s"Unexpected response for One Way Message: ${responseEvenlope.correlationId}")) //FIXME: change with domain specific exception
      }
      context stop self
    case msgSent: SendMessageComplete =>
      reqWithSender.origin ! Done
    case Timeout =>
      reqWithSender.origin ! Status.Failure(new RuntimeException("Timeout exception"))
      context stop self
  }

}

object StreamRequestActor {
  def props(req: StreamRequestWithSender): Props = Props(new StreamRequestActor(req))
}

object protocol {

  case class ResponseInfo(replyTo: String, deserializer: ReactiveDeserializer[_])

  case class StreamRequest(destination: String, payload: String, timeout: Timeout, responseInfo: Option[ResponseInfo])
  case class StreamRequestWithSender(origin: ActorRef, request: StreamRequest)

  case class SendMessageComplete(correlationId: String)
}

case class StreamClientConfig(
  concurrency:      Int,
  bootstrapServers: Set[String],
  responseTopic:    String,
  consumerClientId: String,
  consumerGroupId:  String
)

object StreamClientConfig {
  lazy val default = StreamClientConfig(
    concurrency = 8,
    bootstrapServers = Set("localhost:9092"),
    responseTopic = "responses",
    consumerClientId = "testClient1",
    consumerGroupId = "reactiveSystem"
  )
}
