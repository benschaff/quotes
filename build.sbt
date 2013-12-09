name := "quotes"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  cache,
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.0-SNAPSHOT",
  "org.webjars" % "webjars-play_2.10" % "2.2.1",
  "org.webjars" % "requirejs" % "2.1.8",
  "org.webjars" % "jquery" % "2.0.3-1",
  "org.webjars" % "angularjs" % "1.2.2",
  "org.webjars" % "bootstrap" % "3.0.2",
  "org.webjars" % "highcharts" % "3.0.7",
  "org.webjars" % "cryptojs" % "3.1.2",
  "org.mockito" % "mockito-all" % "1.9.5"
)

play.Project.playScalaSettings
