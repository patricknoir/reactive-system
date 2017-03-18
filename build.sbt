import com.typesafe.sbt.SbtScalariform.ScalariformKeys

import scalariform.formatter.preferences._

// sbt-docker
import com.typesafe.sbt.packager.docker._

// Resolvers
resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots"),
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  Resolver.url("patricknoir ivy resolver", url("http://dl.bintray.com/patricknoir/maven"))(Resolver.ivyStylePatterns)
)

val Versions = new {
  val Scala = "2.12.1"
  val ScalaBinary = "2.12"
  val Slf4j = "1.7.21"
  val Akka = "2.4.17"
  val AkkaHttp = "10.0.4"
  val Circe = "0.7.0"
  val Specs2 = "3.8.9"
  val EmbeddedKafka = "0.12.0"
  val Kafka = "0.10.2.0"
  val KindProjector = "0.9.3"
  val ScalaTest = "3.0.1"
  val AkkaStreamKafka = "0.14"
}

// Dependencies
val compilerPlugins = Seq(
  compilerPlugin("org.spire-math"  %% "kind-projector"      % Versions.KindProjector)
)

val rootDependencies = Seq(
//  "org.slf4j"                      % "slf4j-api"               % Versions.Slf4j,
//  "org.slf4j"                      % "slf4j-log4j12"           % Versions.Slf4j,
  //"com.iheart"                     %% "ficus"                  % "1.2.6",
  "com.typesafe.akka"              %% "akka-slf4j"             % Versions.Akka,
  "com.typesafe.akka"              %% "akka-stream-kafka"      % Versions.AkkaStreamKafka,
  "io.circe"                       %% "circe-core"             % Versions.Circe,
  "io.circe"                       %% "circe-generic"          % Versions.Circe,
  "io.circe"                       %% "circe-parser"           % Versions.Circe
)

val embeddedKafkaDependencies = Seq(
  "org.apache.kafka"               %% "kafka"                    % Versions.Kafka,
  "net.manub"                      %% "scalatest-embedded-kafka" % Versions.EmbeddedKafka
)

val httpInterfaceDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % Versions.AkkaHttp
)

val testDependencies = Seq (
  "org.apache.kafka"               %% "kafka"                    % Versions.Kafka                 % "test",
  "com.typesafe.akka"              %% "akka-stream-testkit"      % Versions.Akka                  % "test",
  "com.typesafe.akka"              %% "akka-http-testkit"        % Versions.AkkaHttp              % "test",
  "org.scalatest"                  %% "scalatest"                % Versions.ScalaTest             % "test",
  "org.specs2"                     %% "specs2-core"              % Versions.Specs2                % "test",
  "org.specs2"                     %% "specs2-scalacheck"        % Versions.Specs2                % "test",
  "org.specs2"                     %% "specs2-mock"              % Versions.Specs2                % "test",
  "net.manub"                      %% "scalatest-embedded-kafka" % Versions.EmbeddedKafka         % "test"
)

val dependencies =
  compilerPlugins ++
    rootDependencies ++
    testDependencies

// Settings
//
val compileSettings = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:_",
  "-target:jvm-1.8",
  "-unchecked",
//  "-Ybackend:GenBCode",
  "-Ydelambdafy:method",
  "-Xfuture",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
//  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

val coverageSettings = Seq(

)

val dockerSettings = Seq(
  defaultLinuxInstallLocation in Docker := "/opt/patricknoir",
  packageName in Docker := packageName.value,
  version in Docker := version.value,
  dockerCommands := Seq(
    Cmd("FROM", "java:openjdk-8-jdk-alpine"),
    Cmd("RUN", "apk upgrade --update && apk add libstdc++ && apk add bash && rm -rf /var/cache/apk/*"),
    Cmd("ADD", "opt /opt"),
    ExecCmd("RUN", "mkdir", "-p", "/var/logs/patricknoir/kafka-reactive-system"),
    ExecCmd("ENTRYPOINT", "/opt/patricknoir/bin/kafka-reactive-system")
  )
)

val forkedJvmOption = Seq(
  "-server",
  "-Dfile.encoding=UTF8",
  "-Duser.timezone=GMT",
  "-Xss1m",
  "-Xms2048m",
  "-Xmx2048m",
  "-XX:+CMSClassUnloadingEnabled",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseConcMarkSweepGC",
  "-XX:+UseParNewGC",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+UseCompressedOops"
)

val jmxJvmOption = Seq (
//  "-Dcom.sun.management.jmxremote",
//  "-Dcom.sun.management.jmxremote.port=9199",
//  "-Dcom.sun.management.jmxremote.rmi.port=9199",
//  "-Dcom.sun.management.jmxremote.local.only=false",
//  "-Dcom.sun.management.jmxremote.authenticate=false",
//  "-Dcom.sun.management.jmxremote.ssl=false"
)

val pluginsSettings =
  coverageSettings ++
  dockerSettings ++
  scalariformSettings

lazy val publishSite = taskKey[Unit]("publish the site under /docs")

val dest = (baseDirectory / "docs")

val commonSettings = Seq(
  organization := "org.patricknoir.kafka",
  version := "0.3.0",
  scalaVersion := "2.12.1",
  fork in run := true,
  fork in Test := true,
  fork in testOnly := true,
  connectInput in run := true,
  javaOptions in run ++= forkedJvmOption ++ jmxJvmOption,
  javaOptions in Test ++= forkedJvmOption,
  scalacOptions := compileSettings,
  ScalariformKeys.preferences := PreferencesImporterExporter.loadPreferences((file(".") / "formatter.preferences").getPath),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := false,
  bintrayReleaseOnPublish := false,
  bintrayOrganization in bintray := None,
  bintrayRepository := "releases"
)


val settings = commonSettings ++ Seq(
  name := "kafka-reactive-service",
  libraryDependencies ++= dependencies,
  unmanagedClasspath in Runtime += baseDirectory.value / "env/local",
  unmanagedClasspath in Compile += baseDirectory.value / "env/console",
  scriptClasspath += "../conf/",
  //mainClass in (Compile, run) := Option("org.patricknoir.kafka.service.Boot"),
  initialCommands in console :=
    """
    | import org.patricknoir.kafka.reactive.client.config._
    | import scala.concurrent.ExecutionContext.Implicits.global
    | import scala.concurrent.duration._
    | import akka.util.Timeout
    | import org.patricknoir.kafka.reactive.client._
    | import akka.actor.ActorSystem
    | import com.typesafe.config.ConfigFactory
    |
    | val akkaConfig = ConfigFactory.load("application.conf")
    |
    | implicit val timeout = Timeout(2 minutes)
    | implicit val system = ActorSystem("testConsole", akkaConfig)
    |
    | val config = KafkaRClientSettings.default
    | val client = new KafkaReactiveClient(config)
    |
    | def quickRequest(dest: String, message: String) = client.request[String, String](s"kafka:$dest/echo", message).onComplete(println)
    |
    """.stripMargin
)

val environment = System.getProperties.getProperty("stage", "local")
val kafkaRSSettings = Seq(
  mappings in Universal ++= {
    (baseDirectory.value / "env" / environment * "*").get.map { f =>
      f -> s"conf/${f.name}"
    }
  }
)


lazy val main =
  project
    .in(file("."))
    .settings(name := "reactive-system")
    .settings(commonSettings:_*)
    .aggregate(kafkaRS, httpInterface, examplesServer, examplesClient, documentation)

lazy val kafkaRS =
  project
    .in(file("kafka-reactive-system"))
    .settings(
       pluginsSettings ++ kafkaRSSettings ++ settings:_*
    )
    .enablePlugins(AshScriptPlugin,JavaServerAppPackaging, UniversalDeployPlugin)

lazy val httpInterface =
  project
    .in(file("http-interface"))
    .dependsOn(kafkaRS)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= httpInterfaceDependencies
    )
    .settings(
      name := "http-interface"
    )

lazy val examplesServer =
  project
    .in(file("examples/server"))
    .dependsOn(kafkaRS)
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++= embeddedKafkaDependencies
    )
    .settings(
      name := "examples-server"
    )

lazy val examplesClient = 
  project
    .in(file("examples/client"))
    .dependsOn(kafkaRS)
    .settings(commonSettings: _*)
    .settings(
      name := "examples-client"
    )

lazy val documentation =
  project
    .in(file("documentation"))
    .dependsOn(kafkaRS, examplesServer)
    .settings(commonSettings:_*)
    .settings(
      name := "documentation",
      paradoxTheme := Some(builtinParadoxTheme("generic")),
      publishSite := Def.task {
        println("Executing task publishSite!")
        val siteDir = (paradox in Compile).value //** "*"
        IO.copyDirectory(siteDir, (baseDirectory / "../docs").value, true)
      }.value
    ).enablePlugins(ParadoxPlugin)