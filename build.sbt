lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.fp",
  scalaVersion := "2.13.2"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.1.2" % "test"

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "sql-fp-pres")
  .aggregate(core)

val doobieVersion = "0.9.0"

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-quill" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      scalaTest
    )
  )
