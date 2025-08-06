package dev.a4i.bsc.modulith.application

import java.io.IOException
import java.util.UUID

import org.apache.hadoop.fs.Path
import zio.*
import zio.config.magnolia.DeriveConfig
import zio.config.magnolia.deriveConfig

case class HadoopFileSystemWorkspace private (id: UUID, path: Path)

object HadoopFileSystemWorkspace:

  def layer: ZLayer[HadoopFileSystem, Config.Error | IOException, HadoopFileSystemWorkspace] =
    ZLayer.scoped(ZIO.acquireRelease(create)(delete))

  private def create: ZIO[HadoopFileSystem, Config.Error | IOException, HadoopFileSystemWorkspace] =
    for
      configuration <- ZIO.config[Configuration]
      hdfs          <- ZIO.service[HadoopFileSystem]
      id             = UUID.randomUUID
      path           = Path(configuration.path, id.toString)
      _             <- hdfs.createDirectories(path)
    yield HadoopFileSystemWorkspace(id, path)

  private def delete(workspace: HadoopFileSystemWorkspace): URIO[HadoopFileSystem, Unit] =
    for
      configuration <- ZIO.config[Configuration].orDie
      hdfs          <- ZIO.service[HadoopFileSystem]
      _             <- ZIO.whenDiscard(configuration.autoClean):
                         hdfs.delete(workspace.path, recursive = true).orDie
    yield ()

  case class Configuration(path: Path, autoClean: Boolean)

  object Configuration:

    private given DeriveConfig[Path] = DeriveConfig[String].map(Path(_))

    given Config[Configuration] = deriveConfig[Configuration].nested("hadoop", "file", "system", "workspaces")
