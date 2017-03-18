package org.patricknoir.kafka.reactive.ex

/**
 * Represents failures occurred in the [[org.patricknoir.kafka.reactive.client.actors.KafkaProducerActor]]
 *
 * Thrown by the producer actor in case of failure and to be handled
 * by the supervisor.
 *
 * TBD Constructor parameters!
 */
class ProducerException(cause: Throwable) extends Exception(cause) {

}
