package org.patricknoir.zk

import java.util.Properties

import org.apache.zookeeper.server.ZooKeeperLocal
import org.specs2.Specification

/**
 * Created by patrick on 16/07/2016.
 */
class ZooKeeperLocalSpec extends Specification {

  def is = s2"""

    testing starting and stopping zk

  """

  @deprecated("I want to test zk and kafka embedded, this is already covered by KafkaServerLocalSpecps")
  def zkStartStop = {
    val zkProperties = new Properties()
    zkProperties.load(this.getClass.getClassLoader.getResourceAsStream("zookeeper.properties"))

    val zk = new ZooKeeperLocal(zkProperties)

    Thread.sleep(5 * 1000) //wait X seconds to properly start up zk

    println("terminating zookeeper")
    zk.stop()

    Thread.sleep(3 * 1000) // wait Y seconds to propertly terminate zk

    true must_== true
  }

}
