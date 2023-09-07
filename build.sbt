name := "gatling-elasticsearch-logs"

version := "1.5.3"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.agido" % "logback-elasticsearch-appender" % "3.0.8",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "ch.qos.logback" % "logback-core" % "1.2.11",
  "org.scalatest" %% "scalatest" % "3.2.12" % "test"
)
