ThisBuild / scalaVersion     := "2.13.18"
ThisBuild / organization     := "hyperlink.navigator"
ThisBuild / organizationName := "hyperlink"

lazy val root = (project in file("."))
  .settings(
    libraryDependencies ++= List(
      "org.typelevel"       %% "cats-core"   % "2.10.0",
      "org.typelevel"       %% "cats-effect" % "3.5.1",
      "co.fs2"              %% s"fs2-core"   % "3.9.2",
      "co.fs2"              %% s"fs2-io"     % "3.9.2",
      "org.scalatest"       %% "scalatest"   % "3.2.20" % Test
    )
  )