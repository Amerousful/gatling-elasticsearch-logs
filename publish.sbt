ThisBuild / organization := "io.github.amerousful"

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

