// Comment to get more information during initialization
//
logLevel := Level.Warn

// Resolvers
//
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

// Dependency graph
//
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")

// Native packager
//
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.5")

// Scalariform
//
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Scoverage
//
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

// Update plugin
//
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

// Bintray plugin
//
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")

addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.2.12")
