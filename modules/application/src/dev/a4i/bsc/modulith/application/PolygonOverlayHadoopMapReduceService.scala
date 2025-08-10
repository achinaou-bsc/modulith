package dev.a4i.bsc.modulith.application

import java.net.URLClassLoader
import java.util.UUID
import scala.reflect.Selectable.reflectiveSelectable

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.hadoop.mapreduce.JobID
import org.apache.hadoop.util.Tool
import org.apache.hadoop.util.ToolRunner
import org.geotools.data.simple.SimpleFeatureCollection
import zio.*
import zio.stream.ZSink

import dev.a4i.bsc.modulith.application.JobArtifactManager.JobArtifact
import dev.a4i.bsc.modulith.application.PolygonOverlayHadoopMapReduceService.ToolDescriptor

class PolygonOverlayHadoopMapReduceService(hdfs: HadoopFileSystem, geoJSONLService: GeoJSONLService):

  def submit(
      jobId: UUID,
      toolDescriptor: ToolDescriptor,
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystemWorkspace & HadoopConfiguration, String] =
    ZIO.logSpan("polygon-overlay-hadoop-map-reduce-service.submit"):
      ZIO.logAnnotate("job-id", jobId.toString):
        for
          _                       <- ZIO.log("Uploading input files...")
          (basePath, overlayPath) <- uploadInputFiles(base, overlay)
          outputPath              <- ZIO.serviceWith[HadoopFileSystemWorkspace]: workspace =>
                                       Path(workspace.path, "output")
          _                       <- ZIO.log("Submitting the job...")
          yarnApplicationId       <- submit(
                                       toolDescriptor,
                                       basePath,
                                       overlayPath,
                                       outputPath,
                                       jobId.toString
                                     ).orDie
          _                       <- ZIO.logAnnotate("yarn-application-id", yarnApplicationId):
                                       ZIO.log(s"Job got Yarn Application Id: ${yarnApplicationId}")
        yield yarnApplicationId

  private def uploadInputFiles(
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystemWorkspace, (basePath: Path, overlayPath: Path)] =
    def upload(featureCollection: SimpleFeatureCollection, path: Path): UIO[Long] =
      ZIO.scoped:
        geoJSONLService
          .encode(FlattenedFeatureCollection(featureCollection))
          .run(ZSink.fromOutputStreamScoped(hdfs.create(path)))
          .orDie

    for
      workspace  <- ZIO.service[HadoopFileSystemWorkspace]
      basePath    = Path(workspace.path, "base.jsonl")
      overlayPath = Path(workspace.path, "overlay.jsonl")
      _          <- upload(base, basePath) <&> upload(overlay, overlayPath)
    yield (basePath, overlayPath)

  private def submit(
      toolDescriptor: ToolDescriptor,
      base: Path,
      overlay: Path,
      output: Path,
      referenceId: String
  ): RIO[HadoopConfiguration, String] =
    for
      jobConfiguration <- ZIO.serviceWith[HadoopConfiguration]: hadoopConfiguration =>
                            new Configuration(hadoopConfiguration):
                              set("mapreduce.job.jar", toolDescriptor.jar.hdfs.toString)
      loader           <- ZIO.attempt:
                            URLClassLoader(
                              Array(toolDescriptor.jar.local.toNIO.toUri.toURL),
                              getClass.getClassLoader
                            )
      tool             <- ZIO.attempt:
                            Class
                              .forName(toolDescriptor.classFqn, true, loader)
                              .getDeclaredConstructor()
                              .newInstance()
                              .asInstanceOf[Tool & { def jobId: Option[JobID] }]
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
                            if exitCode != 0 then throw new RuntimeException(s"Job submission failed with exit code $exitCode")
      applicationId    <- ZIO.attempt:
                            tool.jobId.get.toString.replaceFirst("^job_", "application_")
    yield applicationId.toString

object PolygonOverlayHadoopMapReduceService:

  type Dependencies = HadoopFileSystem & GeoJSONLService

  val layer: RLayer[Dependencies, PolygonOverlayHadoopMapReduceService] =
    ZLayer.derive[PolygonOverlayHadoopMapReduceService]

  case class ToolDescriptor(
      jar: JobArtifact,
      classFqn: String
  )
