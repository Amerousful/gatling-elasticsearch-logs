ThisBuild / organization := "io.github.amerousful"
ThisBuild / scalaVersion := "2.13.8"
ThisBuild / versionScheme := Some("pvp")

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/Amerousful/gatling-elasticsearch-logs"),
    "scm:git:git://github.com/Amerousful/gatling-elasticsearch-logs.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "Amerousful",
    name  = "Pavel Bairov",
    email = "amerousful@gmail.com",
    url   = url("https://github.com/Amerousful")
  )
)

ThisBuild / description := "Send Gatling's logs to Elasticsearch"
ThisBuild / licenses := List("The MIT License (MIT)" -> new URL("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/Amerousful/gatling-elasticsearch-logs"))

ThisBuild / crossPaths := false

ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
