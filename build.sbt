name := "gatling-elasticsearch-logs"

version := "1.6"

scalaVersion := "2.13.8"

libraryDependencies ++= Seq(
  "com.agido" % "logback-elasticsearch-appender" % "3.0.8",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "ch.qos.logback" % "logback-core" % "1.4.7",
  "org.scalatest" %% "scalatest" % "3.2.15" % "test"
)
