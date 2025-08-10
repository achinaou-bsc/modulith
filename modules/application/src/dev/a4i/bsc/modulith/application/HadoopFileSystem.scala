package dev.a4i.bsc.modulith.application

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.Path
import zio.*

class HadoopFileSystem(fileSystem: FileSystem):

  def exists(path: Path): UIO[Boolean] =
    ZIO.attemptBlocking(fileSystem.exists(path)).orDie

  def create(path: Path): URIO[Scope, FSDataOutputStream] =
    ZIO.fromAutoCloseable(ZIO.attemptBlocking(fileSystem.create(path))).orDie

  def createDirectories(path: Path): UIO[Boolean] =
    ZIO.attemptBlocking(fileSystem.mkdirs(path)).orDie

  def delete(path: Path, recursive: Boolean = false): UIO[Boolean] =
    ZIO.attemptBlocking(fileSystem.delete(path, recursive)).orDie

object HadoopFileSystem:

  val layer: URLayer[HadoopConfiguration, HadoopFileSystem] =
    ZLayer.fromFunction: (hadoopConfiguration: HadoopConfiguration) =>
      HadoopFileSystem(FileSystem.get(hadoopConfiguration))
