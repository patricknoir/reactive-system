package org.patricknoir.kafka.reactive.server

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.testkit.TestKit
import org.patricknoir.kafka.reactive.server.dsl._
import org.patricknoir.kafka.reactive.client.actors.KafkaConsumerActor.{ KafkaResponseEnvelope, KafkaResponseStatusCode }
import org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor.KafkaRequestEnvelope
import org.specs2.SpecificationLike
import io.circe.syntax._
import org.patricknoir.kafka.reactive.client.actors.KafkaRClientActor.Destination

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.util.Try

/**
 * Created by patrick on 13/07/2016.
 */
class ReactiveSystemSpec extends TestKit(ActorSystem("TestKit")) with SpecificationLike {

  def is = s2"""

  simple reactive system      $simpleReactiveSystem

  """

  def simpleReactiveSystem = {

    import system.dispatcher

    val destination = Destination("kafka", "destTopic", "echo")

    val source: Source[KafkaRequestEnvelope, _] = Source(1 to 10).map { i =>
      val req = KafkaRequestEnvelope(i.toString, destination, "patrick".asJson.noSpaces, "inboundTopic")
      println(s"Producing request: $req")
      req
    }

    val route: ReactiveRoute = request.sync("echo") {
      (in: String) => s"echo $in"
    } ~ request.sync("length") { (in: String) =>
      in.length
    }
    //    ~ request("toInt") { (in: String) =>
    //      Try {
    //        in.toInt
    //      }
    //    }

    val sink = Sink.seq[Future[KafkaResponseEnvelope]]

    val reactiveSystem = ReactiveSystem(source, route, sink)

    val fResp = (reactiveSystem.run()(ActorMaterializer())).asInstanceOf[Future[Seq[Future[KafkaResponseEnvelope]]]]

    val responses = Await.result(Future.sequence(Await.result(fResp, Duration.Inf)), Duration.Inf)
    println("=======================================")

    responses.map(_.statusCode must be_==(KafkaResponseStatusCode.Success)).reduce(_ and _) and (responses.size must be_==(10))
  }
}
