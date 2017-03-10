package org.patricknoir.kafka.reactive.client.common

import java.net.{URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}

import org.patricknoir.kafka.reactive.common.Destination
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.prop.TableDrivenPropertyChecks

/**
  * Created by michal.losiewicz on 21/09/16.
  */
class DestinationSpec extends FlatSpec with TableDrivenPropertyChecks with Matchers{

  URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory {
    override def createURLStreamHandler(protocol: String): URLStreamHandler = {
      if("kafka".equals(protocol)){
        new URLStreamHandler {
          override def openConnection(u: URL): URLConnection = new URLConnection(u) {
            override def connect(): Unit = println("connected")
          }
        }
      }else{
        null
      }
    }
  })

  val properURLs = Table(
    ("url", "destination"),
    (new URL("http://some.server.com/echo"), Destination("http", "some.server.com", "echo")),
    (new URL("kafka://simple/echo"), Destination("kafka", "simple", "echo"))
  )

  forAll(properURLs) { (url, destination) =>
    Destination.fromURL(url) should equal( destination )
  }

}
