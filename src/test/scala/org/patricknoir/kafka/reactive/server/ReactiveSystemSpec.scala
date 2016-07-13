package org.patricknoir.kafka.reactive.server

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.KafkaResponseEnvelope
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.specs2.SpecificationLike

import io.circe.syntax._

import scala.concurrent.Future

/**
 * Created by patrick on 13/07/2016.
 */
class ReactiveSystemSpec extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

  def is = s2"""


  """

  def simpleReactiveSystem = {

    import ReactiveRoute._

    import system.dispatcher

    val source: Source[KafkaRequestEnvelope, NotUsed] = Source(1 to 10).map { i =>
      KafkaRequestEnvelope(i.toString, "kafka:destTopic/echo", "patrick".asJson.noSpaces, "inboundTopic")
    }

    val route: ReactiveRoute = requestFuture("echo") { (in: String) =>
      s"echo $in"
    } ~ requestFuture("length") { (in: String) =>
      in.length
    }

    val sink: Sink[Future[KafkaResponseEnvelope], _] = Sink.foreach[Future[KafkaResponseEnvelope]] { future =>
      future.onComplete(println)
    }

    val reactiveSystem = ReactiveSystem(source, route, sink)

    reactiveSystem.run()(ActorMaterializer())
  }
}
