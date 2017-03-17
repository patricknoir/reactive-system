package org.patricknoir.kafka.reactive.client.integration

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.{ EmbeddedKafka, EmbeddedKafkaConfig }
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.patricknoir.kafka.reactive.server.ReactiveRoute
import org.specs2.SpecificationLike

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by patrick on 17/03/2017.
 */
abstract class BaseISpec extends TestKit(ActorSystem("TestKit", ConfigFactory.parseString(BaseISpec.configString))) with SpecificationLike {

  def getKafkaPort() = 9092
  def getZooKeeperPort() = 2181

  implicit val materializer = ActorMaterializer()
  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = getKafkaPort(), zooKeeperPort = getZooKeeperPort())

  def startKafka(): Unit = {
    EmbeddedKafka.start()
  }

  def startAtMostOnceServer(route: ReactiveRoute) = KafkaService.atMostOnce(route).run()
  def startAtLeastOnceServer(route: ReactiveRoute) = KafkaService.atLeastOnce(route).run()

  def stopKafka(): Unit = {
    EmbeddedKafka.stop()
  }

}

object BaseISpec {
  val configString = """
                       |akka {
                       |  log-config-on-start = off
                       |
                       |  loggers = ["akka.testkit.TestEventListener"]
                       |  loglevel = "DEBUG"
                       |  stdout-loglevel = "DEBUG"
                       |
                       |  logger-startup-timeout = 10s
                       |  jvm-exit-on-fatal-error = off
                       |
                       |  log-dead-letters = on
                       |  log-dead-letters-during-shutdown = on
                       |
                       |  actor {
                       |    debug {
                       |      autoreceive = on
                       |      receive = on
                       |      lifecycle = on
                       |      fsm = on
                       |      event-stream = on
                       |      unhandled = on
                       |    }
                       |
                       |    custom {
                       |      dispatchers {
                       |        bounded-fork-join-dispatcher {
                       |          type = Dispatcher
                       |          executor = "fork-join-executor"
                       |          mailbox-requirement = "akka.dispatch.BoundedMessageQueueSemantics"
                       |        }
                       |      }
                       |    }
                       |  }
                       |}
                     """.stripMargin
}

class SimpleAtLeastOnceISpec extends BaseISpec {

  def is = s2"""

    simple test with at least once semantic   $runTest

  """

  def runTest = {
    startKafka()
    startAtLeastOnceServer(ServiceCatalog.echo)

    implicit val timeout = Timeout(10 seconds)
    val client = new KafkaReactiveClient(KafkaRClientSettings.default)

    val fResponse = client.request[String, String]("kafka:echoInbound/echo", "patrick")

    val result: String = Await.result(fResponse, Duration.Inf)
    stopKafka()

    result must be_==("patrick")
  }

}
