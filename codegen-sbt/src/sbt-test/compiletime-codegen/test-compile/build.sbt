import sbt.Def.spaceDelimited
import sbt.librarymanagement.Resolver

Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / organization := "Conduktor"
ThisBuild / homepage := Some(url("https://www.conduktor.io/"))
ThisBuild / licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / version := "0.0.1"
ThisBuild / scalaVersion := "2.12.14" // Must stay 2.12 in these tests because the plugin is compiled with 2.12

// ### Dependencies ###

lazy val calibanLib: Seq[ModuleID] =
  sys.props.get("plugin.version") match {
    case Some(x) => Seq("com.github.ghostdogpr" %% "caliban" % x)
    case _       => sys.error("""|The system property 'plugin.version' is not defined.
                           |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  }

lazy val sttp = Seq(
  "com.softwaremill.sttp.client3" %% "core"                          % "3.3.15",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % "3.3.15"
)

// ### App Modules ###

/**
 * `root` is a "meta module". It's the "main module" of this project but doesn't have a physical existence. It represents the "current
 * project" if you prefer, composed of modules.
 *
 * The `aggregate` setting will instruct sbt that when you're launching an sbt command, you want it applied to all the aggregated modules
 */
lazy val root =
  Project(id = "poc_compile_time_caliban_client_generation", base = file("."))
    .aggregate(
      posts,
      potatoes,
      clients,
      calibanClients
    )
    .settings(
      // Additional scripted tests commands
      InputKey[Unit]("copy-file-with-options") := {
        val args: Vector[String] = spaceDelimited("<arg>").parsed.toVector

        IO.copy(
          List(file(args(3)) -> file(args(4))),
          overwrite = args(0).toBoolean,
          preserveLastModified = args(1).toBoolean,
          preserveExecutable = args(2).toBoolean
        )
      },
      InputKey[Unit]("sed-in-place") := {
        val args: Vector[String] = spaceDelimited("<arg>").parsed.toVector

        val previousValue = args(0)
        val newValue      = args(1)
        val baseDir       = baseDirectory.value.getAbsolutePath
        val initialFile   = s"$baseDir/${args(2)}"
        val backupFile    = s"$baseDir/${args(2)}.old"

        IO.move(file(initialFile), file(backupFile))
        val content    = IO.read(file(backupFile))
        val newContent = content.replace(previousValue, newValue)
        IO.write(file(initialFile), newContent)
      }
    )

lazy val posts =
  project
    .in(file("modules/posts"))
    .enablePlugins(CompileTimeCalibanServerPlugin)
    .settings(
      Compile / ctCalibanServer / ctCalibanServerSettings ++=
        Seq(
          "poc.caliban.posts.GraphQLApi.api" -> ClientGenerationSettings.default,
          "poc.caliban.posts.GraphQLApi.api" ->
            ClientGenerationSettings(
              packageName = "poc.caliban.client.generated.posts",
              clientName = "CalibanClient"
            ),
          "poc.caliban.posts.GraphQLApi.api" ->
            ClientGenerationSettings(
              packageName = "poc.caliban.client.generated.posts.split",
              splitFiles = true
            )
        )
    )
    .settings(libraryDependencies ++= calibanLib)

lazy val potatoes =
  project
    .in(file("modules/potatoes"))
    .enablePlugins(CompileTimeCalibanServerPlugin)
    .settings(
      Compile / ctCalibanServer / ctCalibanServerSettings :=
        Seq(
          "poc.caliban.potatoes.PotatoesApi.api" ->
            ClientGenerationSettings(
              packageName = "poc.caliban.client.generated.potatoes",
              splitFiles = true
            )
        )
    )
    .settings(libraryDependencies ++= calibanLib)

lazy val clients =
  project
    .in(file("modules/clients"))
    .settings(libraryDependencies ++= sttp)
    .dependsOn(calibanClients)

lazy val calibanClients =
  project
    .withId("caliban-clients")
    .in(file("modules/caliban-clients"))
    .enablePlugins(CompileTimeCalibanClientPlugin)
    .settings(
      Compile / ctCalibanClient / ctCalibanClientsSettings := Seq(posts, potatoes)
    )
