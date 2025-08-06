package dev.a4i.bsc.modulith.application

import org.apache.hadoop.fs.Path as HadoopFileSystemPath
import os.Path as LocalFileSystemPath
import zio.*
import zio.config.magnolia.DeriveConfig
import zio.config.magnolia.deriveConfig
import zio.stream.ZSink

import dev.a4i.bsc.modulith.application.GitHub.AssetQuery
import dev.a4i.bsc.modulith.application.JobArtifactManager.JobArtifact

class JobArtifactManager(
    gitHub: GitHub,
    hdfs: HadoopFileSystem,
    mutex: Semaphore,
    configuration: JobArtifactManager.Configuration
):

  def get(assetQuery: AssetQuery): Task[JobArtifact] =
    mutex.withPermit:
      for (localPath, hdfsPath) <- download(assetQuery) <&> deploy(assetQuery)
      yield JobArtifact(localPath, hdfsPath)

  private def download(assetQuery: AssetQuery): ZIO[Any, Throwable, LocalFileSystemPath] =
    val jobArtifactPath: LocalFileSystemPath =
      LocalFileSystemPath(s"${configuration.local.path}/${assetQuery.repository}/${assetQuery.tag}/job.jar")

    ZIO
      .ifZIO(ZIO.attemptBlockingIO(os.exists(jobArtifactPath)))(
        ZIO.log("Job artifact is already downloaded"),
        ZIO.scoped:
          for
            _ <- ZIO.log("Downloading job artifact...")
            _ <- ZIO.attemptBlockingIO:
                   os.makeDir.all(jobArtifactPath / os.up)
            _ <- gitHub
                   .streamReleaseAsset(assetQuery)
                   .run(ZSink.fromPath(jobArtifactPath.toNIO))
          yield ()
      )
      .as(jobArtifactPath)

  private def deploy(assetQuery: AssetQuery): ZIO[Any, Throwable, HadoopFileSystemPath] =
    val jobArtifactPath: HadoopFileSystemPath =
      HadoopFileSystemPath(s"${configuration.hdfs.path}/${assetQuery.repository}/${assetQuery.tag}/job.jar")

    ZIO
      .ifZIO(hdfs.exists(jobArtifactPath))(
        ZIO.log("Job artifact is already deployed"),
        ZIO.scoped:
          for
            _ <- ZIO.log("Deploying job artifact...")
            _ <- gitHub
                   .streamReleaseAsset(assetQuery)
                   .run(ZSink.fromOutputStreamScoped(hdfs.create(jobArtifactPath)))
          yield ()
      )
      .as(jobArtifactPath)

object JobArtifactManager:

  val layer: ZLayer[GitHub & HadoopFileSystem, Config.Error, JobArtifactManager] =
    given ZLayer.Derive.Default.WithContext[Any, Nothing, Semaphore] = ZLayer.Derive.Default.fromZIO(Semaphore.make(1))

    ZLayer.derive[JobArtifactManager]

  case class Configuration(local: Configuration.Local, hdfs: Configuration.HDFS)

  object Configuration:

    given Config[Configuration] =
      given DeriveConfig[LocalFileSystemPath]  = DeriveConfig[String].map(LocalFileSystemPath(_))
      given DeriveConfig[HadoopFileSystemPath] = DeriveConfig[String].map(HadoopFileSystemPath(_))

      deriveConfig[Configuration].nested("job", "artifact", "manager")

    case class Local(path: LocalFileSystemPath)

    case class HDFS(path: HadoopFileSystemPath)

  case class JobArtifact(local: LocalFileSystemPath, hdfs: HadoopFileSystemPath)
