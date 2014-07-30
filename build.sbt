import play.PlayScala

import play.PlayImport._

name := "quotes"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.4"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.0-SNAPSHOT",
  "org.webjars" %% "webjars-play" % "2.3.0",
  "org.webjars" % "requirejs" % "2.1.10",
  "org.webjars" % "jquery" % "2.1.0",
  "org.webjars" % "angularjs" % "1.2.19",
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "highcharts" % "4.0.1",
  "org.webjars" % "cryptojs" % "3.1.2",
  "org.mockito" % "mockito-all" % "1.9.5"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

