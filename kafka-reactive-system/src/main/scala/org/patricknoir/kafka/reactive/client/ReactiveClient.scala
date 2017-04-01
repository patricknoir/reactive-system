package org.patricknoir.kafka.reactive.client

import akka.util.Timeout
import org.patricknoir.kafka.reactive.common.{ ReactiveDeserializer, ReactiveSerializer }

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * Client interface to perform requests to a Reactive System Server
 *
 * Used to make requests to a service exposed by Reactive System Server
 *
 * ==Overview==
 *
 * {{{
 *   val client: ReactiveClient = ...
 *
 *   val futureSize: Future[Int] = client.request[String, Int]("length")("hello world")
 *
 * }}}
 *
 */
trait ReactiveClient {

  /**
   * Used to perform requests to a Service exposed by a ReactiveSystem server instance.
   * @param destination string representing a url made with format: 'protocol:topicName/serviceId'
   * @param payload input parameter of the remote service we are invoking
   * @param timeout how long the client has to wait before to expire the request and fail with a timeout error
   * @param ct used by the compiler in order to deserialize the response
   * @tparam In payload input type member of [[org.patricknoir.kafka.reactive.common.ReactiveSerializer]]
   * @tparam Out result type member of [[org.patricknoir.kafka.reactive.common.ReactiveDeserializer]]
   * @return a [[scala.concurrent.Future]] of an [[Out]] in case of success, otherwise a failed [[scala.concurrent.Future]]
   */
  def request[In: ReactiveSerializer, Out: ReactiveDeserializer](destination: String, payload: In)(implicit timeout: Timeout, ct: ClassTag[Out]): Future[Out]

  /**
    * Used to send a unidirectional message to a specific service without expecting any response.
    * The future terminates successful if the producer managed to deliver the message to the broker.
    * * @param destination string representing a url made with format: 'protocol:topicName/serviceId'
    * @param payload input parameter of the remote service we are invoking
    * @param timeout how long the client has to wait before to expire the request and fail with a timeout error
    * @tparam In payload input type member of [[org.patricknoir.kafka.reactive.common.ReactiveSerializer]]
    * @return successful future if the message has been delivered to the broker otherwise it fails.
    */
  def send[In: ReactiveSerializer](destination: String, payload: In, confirmSend: Boolean)(implicit timeout: Timeout): Future[Unit]

}
