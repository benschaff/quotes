import play.PlayScala

import play.PlayImport._

name := "quotes"

version := "1.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.11.4.play23",
  "org.webjars" %% "webjars-play" % "2.3.0-3",
  "org.webjars" % "requirejs" % "2.1.10",
  "org.webjars" % "jquery" % "2.1.0",
  "org.webjars" % "angularjs" % "1.2.19",
  "org.webjars" % "bootstrap" % "3.2.0",
  "org.webjars" % "highcharts" % "4.0.1",
  "org.webjars" % "cryptojs" % "3.1.2",
  "org.mockito" % "mockito-all" % "1.9.5" % Test
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)

