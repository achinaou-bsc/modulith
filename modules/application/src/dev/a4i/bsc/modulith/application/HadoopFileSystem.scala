package dev.a4i.bsc.modulith.application

import java.io.IOException

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.Path
import zio.*

class HadoopFileSystem(fileSystem: FileSystem):

  def exists(path: Path): ZIO[Any, IOException, Boolean] =
    ZIO.attemptBlockingIO(fileSystem.exists(path))

  def create(path: Path): ZIO[Scope, IOException, FSDataOutputStream] =
    ZIO.fromAutoCloseable(ZIO.attemptBlockingIO(fileSystem.create(path)))

  def createDirectories(path: Path): ZIO[Any, IOException, Boolean] =
    ZIO.attemptBlockingIO(fileSystem.mkdirs(path))

  def delete(path: Path, recursive: Boolean = false): ZIO[Any, IOException, Boolean] =
    ZIO.attemptBlockingIO(fileSystem.delete(path, recursive))

object HadoopFileSystem:

  val layer: URLayer[HadoopConfiguration, HadoopFileSystem] =
    ZLayer.fromFunction: (hadoopConfiguration: HadoopConfiguration) =>
      HadoopFileSystem(FileSystem.get(hadoopConfiguration))
