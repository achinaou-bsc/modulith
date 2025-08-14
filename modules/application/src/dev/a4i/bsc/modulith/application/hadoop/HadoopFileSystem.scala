package dev.a4i.bsc.modulith.application.hadoop

import java.io.InputStream

import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.compress.CompressionCodecFactory
import zio.*
import zio.stream.ZStream

class HadoopFileSystem(fileSystem: FileSystem):

  def exists(path: Path): UIO[Boolean] =
    ZIO.attemptBlocking(fileSystem.exists(path)).orDie

  def open(path: Path): URIO[Scope, InputStream] =
    for
      isDirectory <- ZIO.attemptBlocking(fileSystem.getFileStatus(path).isDirectory).orDie
      inputStream <- if isDirectory
                     then openDirectory(path)
                     else openFile(path)
    yield inputStream

  def openDirectory(path: Path): URIO[Scope, InputStream] =
    ZStream
      .fromIterableZIO(listParts(path))
      .flatMap(partPath => ZStream.fromInputStreamZIO(open(partPath)))
      .toInputStream
      .orDie

  def openFile(path: Path): URIO[Scope, InputStream] =
    ZIO
      .attemptBlocking:
        val rawInputStream   = fileSystem.open(path)
        val compressionCodec = CompressionCodecFactory(fileSystem.getConf).getCodec(path)

        if compressionCodec != null
        then compressionCodec.createInputStream(rawInputStream)
        else rawInputStream
      .withFinalizerAuto
      .orDie

  def listParts(path: Path): UIO[Vector[Path]] =
    ZIO
      .attemptBlocking:
        fileSystem
          .listStatus(path)
          .sortBy(_.getPath.getName)
          .iterator
          .filter(fileStatus => !fileStatus.isDirectory && fileStatus.getPath.getName.startsWith("part-"))
          .map(_.getPath)
          .toVector
      .orDie

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
