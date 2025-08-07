package dev.a4i.bsc.modulith.application

import java.net.URLClassLoader
import scala.reflect.Selectable.reflectiveSelectable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.util.Tool
import org.apache.hadoop.util.ToolRunner
import org.geotools.data.simple.SimpleFeatureCollection
import zio.*
import zio.config.magnolia.deriveConfig
import zio.stream.ZSink

import dev.a4i.bsc.modulith.application.JobArtifactManager.JobArtifact

class PolygonOverlayHadoopMapReduceNaiveService(
    jobArtifactManager: JobArtifactManager,
    hdfs: HadoopFileSystem,
    geoJSONLService: GeoJSONLService,
    configuration: PolygonOverlayHadoopMapReduceNaiveService.Configuration
):

  def run(
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): RIO[HadoopFileSystemWorkspace & HadoopConfiguration, String] =
    for
      workspace               <- ZIO.service[HadoopFileSystemWorkspace]
      _                       <- ZIO.log("Uploading input files...")
      (basePath, overlayPath) <- uploadInputFiles(base, overlay)
      outputPath               = Path(workspace.path, "output")
      referenceId              = workspace.id.toString
      _                       <- ZIO.log("Ensuring job artifacts...")
      jobArtifact             <- ensureJobArtifacts
      _                       <- ZIO.log("Running the job...")
      jobId                   <- run(
                                   jobArtifact,
                                   "dev.a4i.bsc.polygon.overlay.hadoop.mapreduce.naive.PolygonOverlayHadoopMapReduceNaive",
                                   basePath,
                                   overlayPath,
                                   outputPath,
                                   referenceId
                                 )
      _                       <- ZIO.log(s"Job's id is: ${jobId}")
    yield jobId

  private def uploadInputFiles(
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): RIO[HadoopFileSystemWorkspace, (basePath: Path, overlayPath: Path)] =
    def upload(featureCollection: SimpleFeatureCollection, path: Path): Task[Long] =
      ZIO.scoped:
        geoJSONLService
          .encode(FlattenedFeatureCollection(featureCollection))
          .run(ZSink.fromOutputStreamScoped(hdfs.create(path)))

    for
      workspace  <- ZIO.service[HadoopFileSystemWorkspace]
      basePath    = Path(workspace.path, "base.jsonl")
      overlayPath = Path(workspace.path, "overlay.jsonl")
      _          <- upload(base, basePath) <&> upload(overlay, overlayPath)
    yield (basePath, overlayPath)

  private def ensureJobArtifacts: Task[JobArtifact] =
    jobArtifactManager.get(GitHub.AssetQuery(
      configuration.token,
      "achinaou-bsc",
      "polygon-overlay-hadoop-mapreduce-naive",
      configuration.tag,
      "polygon-overlay-hadoop-mapreduce-naive.jar"
    ))

  private def run(
      jobArtifact: JobArtifact,
      jobClass: String,
      base: Path,
      overlay: Path,
      output: Path,
      referenceId: String
  ): RIO[HadoopConfiguration, String] =
    for
      jobConfiguration <- ZIO.serviceWith[HadoopConfiguration]: hadoopConfiguration =>
                            new Configuration(hadoopConfiguration):
                              set("mapreduce.job.jar", jobArtifact.hdfs.toString)
      loader           <- ZIO.attempt:
                            URLClassLoader(
                              Array(jobArtifact.local.toNIO.toUri.toURL),
                              getClass.getClassLoader
                            )
      tool             <- ZIO.attempt:
                            Class
                              .forName(jobClass, true, loader)
                              .getDeclaredConstructor()
                              .newInstance()
                              .asInstanceOf[Tool & { def jobId: Option[String] }]
      arguments         = Array(
                            "--base",
                            base.toString,
                            "--overlay",
                            overlay.toString,
                            "--output",
                            output.toString,
                            "--reference-id",
                            referenceId,
                            "--wait-for-completion",
                            "false"
                          )
      _                <- ZIO.attempt:
                            val exitCode: Int = ToolRunner.run(jobConfiguration, tool, arguments)
                            if exitCode != 0 then throw new RuntimeException(s"Job failed with exit code $exitCode")
      jobId            <- ZIO.attempt:
                            tool.jobId.get
    yield jobId

object PolygonOverlayHadoopMapReduceNaiveService:

  type Dependencies = JobArtifactManager & HadoopFileSystem & GeoJSONLService

  val layer: RLayer[Dependencies, PolygonOverlayHadoopMapReduceNaiveService] =
    ZLayer.derive[PolygonOverlayHadoopMapReduceNaiveService]

  case class Configuration(
      token: String,
      tag: String
  )

  object Configuration:

    given Config[Configuration] =
      deriveConfig[Configuration].nested("polygon", "overlay", "hadoop", "mapreduce", "naive")
