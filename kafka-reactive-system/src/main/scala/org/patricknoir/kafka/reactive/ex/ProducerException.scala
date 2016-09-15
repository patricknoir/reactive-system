package org.patricknoir.kafka.reactive.ex

/**
 * Created by patrick on 23/08/2016.
 *
 * Thrown by the producer actor in case of failure and to be handled
 * by the supervisor.
 *
 * TBD Constructor parameters!
 */
class ProducerException(cause: Throwable) extends Exception(cause) {

}
