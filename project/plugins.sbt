// Comment to get more information during initialization
//
logLevel := Level.Warn

// Resolvers
//
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

// Dependency graph
//
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.1")

// Native packager
//
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.6")

// Scalariform
//
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// Scoverage
//
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.0-M3")

// Update plugin
//
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.4")

// Bintray plugin
//
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")

addSbtPlugin("com.lightbend.paradox" % "sbt-paradox" % "0.3.5")
