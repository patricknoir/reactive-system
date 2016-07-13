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
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.0")

// Scalariform
//
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

// Scoverage
//
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

// Update plugin
//
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.10")
