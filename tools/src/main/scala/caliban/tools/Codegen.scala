package caliban.tools

import caliban.tools.implicits.ScalarMappings
import zio.blocking.{ effectBlocking, Blocking }
import zio.{ RIO, Task, ZIO }

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file._

object Codegen {

  sealed trait GenType

  object GenType {
    object Schema extends GenType

    object Client extends GenType
  }

  def generate(
    loader: SchemaLoader,
    arguments: Options,
    genType: GenType
  ): RIO[Blocking, List[File]] =
    for {
      schema            <- loader.load
      s                  = ".*/[scala|play.*|app][^/]*/(.*)/(.*).scala".r.findFirstMatchIn(arguments.toPath)
      packageName        = arguments.packageName.orElse(s.map(_.group(1).split("/").mkString(".")))
      objectName         = arguments.clientName.orElse(s.map(_.group(2))).getOrElse("Client")
      abstractEffectType = arguments.abstractEffectType.getOrElse(false)
      effect             = arguments.effect.getOrElse {
                             if (abstractEffectType) "F" else "zio.UIO"
                           }
      genView            = arguments.genView.getOrElse(false)
      scalarMappings     = arguments.scalarMappings
      splitFiles         = arguments.splitFiles.getOrElse(false)
      enableFmt          = arguments.enableFmt.getOrElse(true)
      extensibleEnums    = arguments.extensibleEnums.getOrElse(false)
      code               = genType match {
                             case GenType.Schema =>
                               List(
                                 objectName -> SchemaWriter.write(schema, packageName, effect, arguments.imports, abstractEffectType)(
                                   ScalarMappings(scalarMappings)
                                 )
                               )
                             case GenType.Client =>
                               ClientWriter.write(
                                 schema,
                                 objectName,
                                 packageName,
                                 genView,
                                 arguments.imports,
                                 splitFiles,
                                 extensibleEnums
                               )(
                                 ScalarMappings(scalarMappings)
                               )
                           }
      formatted         <- if (enableFmt) Formatter.format(code, arguments.fmtPath) else Task.succeed(code)
      paths             <- ZIO.foreach(formatted) { case (objectName, objectCode) =>
                             effectBlocking {
                               val path = Path.of(
                                 if (splitFiles) s"${arguments.toPath.reverse.dropWhile(_ != '/').reverse}$objectName.scala"
                                 else arguments.toPath
                               )

                               Files.writeString(path, objectCode, StandardCharsets.UTF_8).toFile
                             }
                           }
    } yield paths

  def generate(arguments: Options, genType: GenType): RIO[Blocking, List[File]] =
    generate(
      getSchemaLoader(arguments.schemaPath, arguments.headers),
      arguments,
      genType
    )

  private def getSchemaLoader(path: String, schemaPathHeaders: Option[List[Options.Header]]): SchemaLoader =
    if (path.startsWith("http")) SchemaLoader.fromIntrospection(path, schemaPathHeaders)
    else SchemaLoader.fromFile(path)

}
