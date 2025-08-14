package dev.a4i.bsc.modulith.application.hadoop

import java.util.UUID

import org.apache.hadoop.fs.Path
import zio.*
import zio.config.magnolia.DeriveConfig
import zio.config.magnolia.deriveConfig

case class HadoopFileSystemWorkspace private (path: Path)

object HadoopFileSystemWorkspace:

  def layer(id: UUID): URLayer[HadoopFileSystem, HadoopFileSystemWorkspace] =
    ZLayer.scoped(ZIO.acquireRelease(create(id))(delete))

  private def create(id: UUID): URIO[HadoopFileSystem, HadoopFileSystemWorkspace] =
    for
      configuration <- ZIO.config[Configuration].orDie
      hdfs          <- ZIO.service[HadoopFileSystem]
      path           = Path(configuration.path, id.toString)
      _             <- hdfs.createDirectories(path)
    yield HadoopFileSystemWorkspace(path)

  private def delete(workspace: HadoopFileSystemWorkspace): URIO[HadoopFileSystem, Unit] =
    ZIO.unit

  case class Configuration(path: Path)

  object Configuration:

    private given DeriveConfig[Path] = DeriveConfig[String].map(Path(_))

    given Config[Configuration] = deriveConfig[Configuration].nested("hadoop", "file", "system", "workspaces")
