package caliban.tools.compiletime

import caliban.tools.CalibanCommonSettings
import caliban.tools.compiletime.Config._
import zio.test._
import zio.test.environment.TestEnvironment

object ConfigSpec extends DefaultRunnableSpec {

  private val fullExample: ClientGenerationSettings =
    ClientGenerationSettings(
      packageName = "io.example.generated",
      clientName = "CalibanClient",
      scalafmtPath = Some("a/b/c"),
      genView = true,
      scalarMappings = List(("mapping key 1", "mapping value 1"), "abc" -> "def"),
      imports = List("zio.test._", "caliban.tools.compiletime._"),
      splitFiles = true,
      enableFmt = false,
      extensibleEnums = true,
      excludeClientDeprecation = false
    )

  private val fullExampleWithDeprecation: ClientGenerationSettings =
    ClientGenerationSettings(
      packageName = "io.example.generated",
      clientName = "CalibanClient",
      scalafmtPath = Some("a/b/c"),
      genView = true,
      scalarMappings = List(("mapping key 1", "mapping value 1"), "abc" -> "def"),
      imports = List("zio.test._", "caliban.tools.compiletime._"),
      splitFiles = true,
      enableFmt = false,
      extensibleEnums = true,
      excludeClientDeprecation = true
    )

  private val toCalibanCommonSettingsSpec =
    suite("#toCalibanCommonSettings")(
      test("fullExample")(
        assertTrue(
          fullExample.toCalibanCommonSettings ==
            CalibanCommonSettings(
              clientName = Some("CalibanClient"),
              scalafmtPath = Some("a/b/c"),
              headers = List.empty,
              packageName = Some("io.example.generated"),
              genView = Some(true),
              scalarMappings = List(("mapping key 1", "mapping value 1"), "abc" -> "def"),
              imports = List("zio.test._", "caliban.tools.compiletime._"),
              splitFiles = Some(true),
              enableFmt = Some(false),
              extensibleEnums = Some(true),
              excludeClientDeprecation = Some(false)
            )
        )
      )
    )

  private val toCalibanCommonSettingsWithDeprecationSpec =
    suite("#toCalibanCommonSettingsWithDeprecation")(
      test("fullExampleWithDeprecation")(
        assertTrue(
          fullExampleWithDeprecation.toCalibanCommonSettings ==
            CalibanCommonSettings(
              clientName = Some("CalibanClient"),
              scalafmtPath = Some("a/b/c"),
              headers = List.empty,
              packageName = Some("io.example.generated"),
              genView = Some(true),
              scalarMappings = List(("mapping key 1", "mapping value 1"), "abc" -> "def"),
              imports = List("zio.test._", "caliban.tools.compiletime._"),
              splitFiles = Some(true),
              enableFmt = Some(false),
              extensibleEnums = Some(true),
              excludeClientDeprecation = Some(true)
            )
        )
      )
    )

  private val asScalaCodeSpec =
    suite("#asScalaCode")(
      test("default")(
        assertTrue(
          ClientGenerationSettings.default.asScalaCode ==
            s"""
               |ClientGenerationSettings(
               |  packageName = "generated",
               |  clientName = "Client",
               |  scalafmtPath = None,
               |  genView = false,
               |  scalarMappings = List.empty,
               |  imports = List.empty,
               |  splitFiles = false,
               |  enableFmt = true,
               |  extensibleEnums = false,
               |  excludeClientDeprecation = false
               |)
            """.stripMargin.trim
        )
      ),
      test("full example")(
        assertTrue(
          fullExample.asScalaCode ==
            s"""
               |ClientGenerationSettings(
               |  packageName = "io.example.generated",
               |  clientName = "CalibanClient",
               |  scalafmtPath = Some("a/b/c"),
               |  genView = true,
               |  scalarMappings = List(("mapping key 1","mapping value 1"),("abc","def")),
               |  imports = List("zio.test._","caliban.tools.compiletime._"),
               |  splitFiles = true,
               |  enableFmt = false,
               |  extensibleEnums = true,
               |  excludeClientDeprecation = false
               |)
            """.stripMargin.trim
        )
      )
    )

  private val defaultSpec =
    suite(".default")(
      test("use default value")(
        assertTrue(
          ClientGenerationSettings.default ==
            ClientGenerationSettings(
              packageName = "generated",
              clientName = "Client",
              scalafmtPath = None,
              genView = false,
              scalarMappings = List.empty,
              imports = List.empty,
              splitFiles = false,
              enableFmt = true,
              extensibleEnums = false,
              excludeClientDeprecation = false
            )
        )
      )
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Config spec")(
      suite("ClientGenerationSettings")(
        toCalibanCommonSettingsSpec,
        asScalaCodeSpec,
        defaultSpec,
        toCalibanCommonSettingsWithDeprecationSpec
      )
    )
}
