# Client

@@toc{ depth=3 }

## Overview

The Reactive System client allows applications to consume remote services using asynchronous
message exchange pattern exposed by a Reactive System server.

In order to consume a remote service all you need is an instance of `ReactiveClient`:

@@snip [SimpleRSClient.scala](../../../../examples/client/src/main/scala/org/patricknoir/kafka/examples/client/SimpleRSClient.scala) { #reactive-client-create-client }

## Simple Request

We are now going to create a simple client which will call the Reactive System server we have 
created in the previous section.

For clarity here is the code snipped for the **server** definition:

@@snip [SimpleRSServer.scala](../../../../examples/server/src/main/scala/org/patricknoir/kafka/examples/server/SimpleRSServer.scala) { #reactive-system-at-least-once }

As we can see the **server** exposes 2 services:

* incrementCounter - accepts a `step:Int` and returns `Unit`
* getCounter - doesn't accept any parameter and returns `Int`

The services are bound to the kafka topic: `simple` as defined by the Kafka Source used to create
the `ReactiveSystem` instance.

Here is the client code in order to invoke `incrementCounter`:

@@snip [SimpleRSClient.scala](../../../../examples/client/src/main/scala/org/patricknoir/kafka/examples/client/SimpleRSClient.scala) { #reactive-client-call-get-counter }

## One-Way Simple Request

The Reactive System client also allows to send a message to a remote service without awaiting for a reply from the target service.

The following snippet shows how to invoke the API in order to send a message to the `incrementCounter` service without awaiting for a response:

@@snip [SimpleRSClient.scala](../../../../examples/client/src/main/scala/org/patricknoir/kafka/examples/client/SimpleRSClient.scala) { #reactive-client-one-way-message }

Please note even though this is a fire-and-forget way of sending a message, the API allows to be confirmed whether the message being successfully sent by setting the `confirmSend` flag.
