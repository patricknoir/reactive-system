package org.patricknoir.client.config

import org.patricknoir.kafka.reactive.client.config.KafkaRClientSettings
import org.specs2.Specification

/**
 * Created by patrick on 16/07/2016.
 */
class KafkaRClientSettingsSpec extends Specification {

  def is = s2"""

    test parsing configuration $parseConfig

  """

  def parseConfig = {
    val config = KafkaRClientSettings.default

    config.producerSettings.isEmpty must_!= (true) and
      config.consumerSettings.isEmpty must_!= (true)
  }

}
