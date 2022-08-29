name := "gatling-elasticsearch-logs"

version := "0.9.1"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.internetitem" % "logback-elasticsearch-appender" % "1.6",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "ch.qos.logback" % "logback-core" % "1.2.11",
  "org.scalatest" %% "scalatest" % "3.2.12" % "test"
)
