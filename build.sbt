lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill.fp",
  scalaVersion := "2.12.8"
)

val scalaTest = "org.scalatest" %% "scalatest" % "3.0.7" % "test"

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "sql-fp-pres")
  .aggregate(core)

val doobieVersion = "0.8.6"

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-quill" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      scalaTest
    )
  )
