name := "gatling-elasticsearch-logs"

version := "0.2"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.internetitem" % "logback-elasticsearch-appender" % "1.6",
  "ch.qos.logback" % "logback-classic" % "1.2.5",
  "ch.qos.logback" % "logback-core" % "1.2.5",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)
