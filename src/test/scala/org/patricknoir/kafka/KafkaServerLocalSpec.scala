package org.patricknoir.kafka

import java.util.Properties

import org.specs2.Specification

/**
 * Created by patrick on 16/07/2016.
 */
class KafkaServerLocalSpec extends Specification {

  def is = s2"""

    starting and stopping kafka     $kafkaStartStop

  """

  def kafkaStartStop = {
    val zkProperties = new Properties()
    val kkProperties = new Properties()

    zkProperties.load(this.getClass.getClassLoader.getResourceAsStream("zookeeper.properties"))
    kkProperties.load(this.getClass.getClassLoader.getResourceAsStream("server.properties"))

    val kafka = new KafkaLocal(kkProperties, zkProperties)

    Thread.sleep(5 * 1000) //wait for X seconds to start up kafka

    println("shutting down kafka")
    kafka.stop()

    Thread.sleep(5 * 1000)

    true must_== true
  }

}
