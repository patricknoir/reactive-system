// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `reactive-system` =
  project
    .in(file("."))
    .enablePlugins(SbtScalariform)
    .aggregate(
      `kafka-reactive-system`,
      `http-interface`,
      `examples-server`,
      `examples-client`,
      `documentation`
    )
    .settings(settings)
    .settings(
      name := "reactive-system",
      Compile / unmanagedSourceDirectories := Seq.empty,
      Test / unmanagedSourceDirectories := Seq.empty,
      publishArtifact := false
    )

lazy val `examples-server` =
  project
    .in(file("examples/server"))
    .settings(
      name := "examples-server",
      libraryDependencies ++= embeddedKafkaDependencies
    )
    .settings(settings)
    .dependsOn(`kafka-reactive-system`)

lazy val `examples-client` =
  project
    .in(file("examples/client"))
    .dependsOn(`kafka-reactive-system`)
    .settings(
      name := "example-client"
    )
    .settings(settings)

lazy val `documentation` =
  project
    .dependsOn(`kafka-reactive-system`, `examples-server`, `examples-client`)
    .settings(settings)
    .settings(
      name := "documentation"
    )
    .settings(
      paradoxTheme := Some(builtinParadoxTheme("generic")),
      paradoxProperties in Compile ++= Map(
        "scaladoc.rfc.base_url" -> s"https://patricknoir.github.io/reactive-system/api/%s.html"
      ),
      paradoxNavigationDepth := 3,
      publishSite := Def.task {
        println("Executing task publishSite!")
        val docsDir= baseDirectory.value / "../docs"
        val internalApiDir = baseDirectory.value / "../docs/api"
        val apidocsDir = baseDirectory.value / "../apidocs"
        val siteDir = (paradox in Compile).value //** "*"
        IO.delete(docsDir)
        IO.createDirectory(docsDir)
        IO.copyDirectory(siteDir, docsDir, true)
        IO.copyDirectory(apidocsDir, internalApiDir, true)
      }.value
    ).enablePlugins(ParadoxPlugin)

lazy val `kafka-reactive-system` =
  project
    .enablePlugins(
      AshScriptPlugin,
      JavaServerAppPackaging,
      UniversalDeployPlugin
    )
    .settings(`kafka-reactive-system-settings`)
    .settings(
      name := "kafka-reactive-system",
      settings
    )
    .settings(
      publishSite := Def.task {
        println("Executing task publishSite!")
        val apidocsDir = baseDirectory.value / "../apidocs"
        IO.delete(apidocsDir)
        IO.createDirectory(apidocsDir)
        IO.copyDirectory((doc in Compile).value, apidocsDir, true)
      }.value
    )

lazy val `http-interface` = project
  .dependsOn(`kafka-reactive-system`)
  .settings(
    name := "http-interface",
    libraryDependencies ++= httpInterfaceDependencies,
    commonSettings
  )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

val Versions = new {
  val Scala = "2.12.6"
  val ScalaBinary = "2.12"
  val Slf4j = "1.7.21"
  val ScalaLogging = "3.9.0"
  val Akka = "2.5.14"
  val AkkaHttp = "10.1.3"
  val Circe = "0.9.3"
  val Specs2 = "3.8.9"
  val EmbeddedKafka = "1.1.1"
  val Kafka = "2.0.0"
  val KindProjector = "0.9.3"
  val ScalaTest = "3.0.1"
  val AkkaStreamKafka = "0.22"
}

// Dependencies
val compilerPlugins = Seq(
  compilerPlugin("org.spire-math" %% "kind-projector" % Versions.KindProjector)
)

val rootDependencies = Seq(
  //  "org.slf4j"                      % "slf4j-api"               % Versions.Slf4j,
  //  "org.slf4j"                      % "slf4j-log4j12"           % Versions.Slf4j,
  //"com.iheart"                     %% "ficus"                  % "1.2.6",
  "com.typesafe.scala-logging" %% "scala-logging" % Versions.ScalaLogging,
  "com.typesafe.akka" %% "akka-slf4j" % Versions.Akka,
  "com.typesafe.akka" %% "akka-stream-kafka" % Versions.AkkaStreamKafka,
  "io.circe" %% "circe-core" % Versions.Circe,
  "io.circe" %% "circe-generic" % Versions.Circe,
  "io.circe" %% "circe-parser" % Versions.Circe
)

val embeddedKafkaDependencies = Seq(
  "org.apache.kafka" %% "kafka" % Versions.Kafka,
  "net.manub" %% "scalatest-embedded-kafka" % Versions.EmbeddedKafka
)
//
val httpInterfaceDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % Versions.AkkaHttp
)

val testDependencies = Seq(
  "org.apache.kafka" %% "kafka" % Versions.Kafka % "test",
  "com.typesafe.akka" %% "akka-stream-testkit" % Versions.Akka % "test",
  "com.typesafe.akka" %% "akka-http-testkit" % Versions.AkkaHttp % "test",
  "org.scalatest" %% "scalatest" % Versions.ScalaTest % "test",
  "org.specs2" %% "specs2-core" % Versions.Specs2 % "test",
  "org.specs2" %% "specs2-scalacheck" % Versions.Specs2 % "test",
  "org.specs2" %% "specs2-mock" % Versions.Specs2 % "test",
  "net.manub" %% "scalatest-embedded-kafka" % Versions.EmbeddedKafka % "test"
)

val dependencies =
  compilerPlugins ++
    rootDependencies ++
    testDependencies

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++ Seq(
    libraryDependencies ++= dependencies,
    unmanagedClasspath in Runtime += baseDirectory.value / "env/local",
    unmanagedClasspath in Compile += baseDirectory.value / "env/console",
//    scriptClasspath += "../conf/",
//    mainClass in (Compile, run) := Option("org.patricknoir.kafka.service.Boot"),
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

lazy val commonSettings = Seq(
  organization := "org.patricknoir.kafka",
  version := "0.3.0",
  scalaVersion in ThisBuild := "2.12.6",
  fork in run := true,
  fork in Test := true,
  fork in testOnly := true,
  connectInput in run := true,
  javaOptions in run ++= forkedJvmOption ++ jmxJvmOption,
  javaOptions in Test ++= forkedJvmOption,
  scalacOptions := compileSettings,
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  publishMavenStyle := true,
  publishArtifact := true,
  publishArtifact in Test := false,
  bintrayReleaseOnPublish := false,
  bintrayOrganization in bintray := None,
  bintrayRepository := "releases"
)

val compileSettings = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
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

val jmxJvmOption = Seq(
//  "-Dcom.sun.management.jmxremote",
//  "-Dcom.sun.management.jmxremote.port=9199",
//  "-Dcom.sun.management.jmxremote.rmi.port=9199",
//  "-Dcom.sun.management.jmxremote.local.only=false",
//  "-Dcom.sun.management.jmxremote.authenticate=false",
//  "-Dcom.sun.management.jmxremote.ssl=false"
)

val pluginsSettings = {

  val coverageSettings = Seq(
    coverageMinimum := 70,
    coverageFailOnMinimum := false,
    coverageHighlighting := true
  )

  val scalariformSettings = {
    import com.typesafe.sbt.SbtScalariform.ScalariformKeys._

    lazy val BuildConfig = config("build") extend Compile
    lazy val BuildSbtConfig = config("buildsbt") extend Compile

    includeFilter in (BuildConfig, format) := ("*.scala": FileFilter)

    includeFilter in (BuildSbtConfig, format) := ("*.sbt": FileFilter)
  }

  val dockerSettings = {
    import com.typesafe.sbt.packager.docker._

    Seq(
      defaultLinuxInstallLocation in Docker := "/opt/patricknoir",
      packageName in Docker := name.value,
      version in Docker := version.value,
      dockerCommands := Seq(
        Cmd("FROM", "java:openjdk-8-jdk-alpine"),
        Cmd(
          "RUN",
          "apk upgrade --update && apk add libstdc++ && apk add bash && rm -rf /var/cache/apk/*"),
        Cmd("ADD", "opt /opt"),
        ExecCmd("RUN",
                "mkdir",
                "-p",
                "/var/logs/patricknoir/kafka-reactive-system"),
        ExecCmd("ENTRYPOINT", "/opt/patricknoir/bin/kafka-reactive-system")
      )
    )
  }

  coverageSettings ++ dockerSettings ++ scalariformSettings
}

val environment = System.getProperties.getProperty("stage", "local")

val `kafka-reactive-system-settings` = Seq(
  mappings in Universal ++= {
    (baseDirectory.value / "env" / environment * "*").get.map { f =>
      f -> s"conf/${f.name}"
    }
  }
)

// Resolvers
resolvers ++= Seq(
  Resolver.jcenterRepo,
  Resolver.sonatypeRepo("snapshots"),
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  Resolver.url(
    "patricknoir ivy resolver",
    url("http://dl.bintray.com/patricknoir/maven"))(Resolver.ivyStylePatterns),
  Resolver.url("scoverage-bintray",
               url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(
    Resolver.ivyStylePatterns)
)

lazy val publishSite = taskKey[Unit]("publish the site under /docs")
