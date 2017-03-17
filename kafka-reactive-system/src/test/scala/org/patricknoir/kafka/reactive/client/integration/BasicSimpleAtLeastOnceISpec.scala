package org.patricknoir.kafka.reactive.client.integration

import akka.util.Timeout
import org.patricknoir.kafka.reactive.client.KafkaReactiveClient
import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by patrick on 17/03/2017.
 */
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
