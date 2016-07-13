Reactive Kafka Service
======================

Current Version: **1.0.0-SNAPSHOT**

Introduction
------------
This project aims to provide a new paradigm to implement Reactive Service which are message driven and purely reactive.
This library will enable you to declare server side services exposed by your application using a DSL similar to Akka HTTP
using Kafka Topics rather than http requests/responses.
This project also provides a client library to consume those services which will hide the publisher/consumer by simple function
invocations that returns Future[_] making easy to compose different requests together.

To Fix
------

1. Replace io.circe.Encoder[A]/Decoder[A] typeclasses from the API and replace by a custom typeclass for kafka serialization/deserialization. We will offer implicit converters from/to circe Decoders/Encoders
2. Generalise the API from Kafka such as it can be implemented with any messaging system
3. FIXME: I have been messing with (Error Xor A) (Error = java.lang.Error) => this should be replaced with Throwable or custom Error type
4. Replace ReactiveRoute.request* DSL methods with Magnet Pattern like in Akka HTTP
5. ReactiveSystem implementation is too naive and the only purpose is to show the API usage
6. Implement Semantics At Least Once for Command/Event pattern