package dev.a4i.bsc.modulith.application.service

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

import dev.a4i.bsc.modulith.application.geo.FlattenedFeatureCollection
import dev.a4i.bsc.modulith.application.geo.GeoJSONSequenceService
import dev.a4i.bsc.modulith.application.hadoop.HadoopConfiguration
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.service.JobArtifactManager.JobArtifact
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceService.ToolDescriptor

class PolygonOverlayHadoopMapReduceService(
    geoJSONSequenceService: GeoJSONSequenceService,
    hdfs: HadoopFileSystem,
    hadoopConfiguration: HadoopConfiguration
):

  def submit(
      jobId: UUID,
      toolDescriptor: ToolDescriptor,
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystemWorkspace, String] =
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
                                 )
      _                       <- ZIO.log(s"Job got Yarn Application Id: ${yarnApplicationId}")
    yield yarnApplicationId

  private def uploadInputFiles(
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystemWorkspace, (basePath: Path, overlayPath: Path)] =
    def upload(featureCollection: SimpleFeatureCollection, path: Path): UIO[Long] =
      ZIO.scoped:
        geoJSONSequenceService
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
  ): UIO[String] =
    for
      jobConfiguration = new Configuration(hadoopConfiguration):
                           set("mapreduce.job.jar", toolDescriptor.jar.hdfs.toString)

                           // OPTIMAL: 8 reducers for 4-node cluster
                           setInt("mapreduce.job.reduces", 8)
                           setInt("mapreduce.reduce.memory.mb", 10240)    // 10GB per reducer
                           set("mapreduce.reduce.java.opts", "-Xmx8192m") // 8GB heap
                           setInt("mapreduce.map.memory.mb", 6144)        // 6GB per mapper
                           set("mapreduce.map.java.opts", "-Xmx4608m")    // 4.5GB heap

                           // Shuffle settings (can be less aggressive with 8 vs 4)
                           setInt("mapreduce.reduce.shuffle.maxfetchfailures", 300)
                           setInt("mapreduce.reduce.shuffle.connect.timeout", 900000) // 15 min
                           setInt("mapreduce.reduce.shuffle.read.timeout", 900000)    // 15 min
                           setInt("mapreduce.reduce.shuffle.parallelcopies", 3)       // Slightly more aggressive
                           setInt("mapreduce.task.timeout", 3600000)                  // 1 hour per task

                           // Anti-spilling settings
                           setFloat("mapreduce.map.sort.spill.percent", 0.9f)
                           setFloat("mapreduce.reduce.shuffle.merge.percent", 0.9f)
                           setFloat("mapreduce.reduce.shuffle.memory.limit.percent", 0.8f)
      loader          <- ZIO
                           .attempt:
                             URLClassLoader(Array(toolDescriptor.jar.local.toNIO.toUri.toURL), getClass.getClassLoader)
                           .orDie
      tool            <- ZIO
                           .attempt:
                             Class
                               .forName(toolDescriptor.classFqn, true, loader)
                               .getDeclaredConstructor()
                               .newInstance()
                               .asInstanceOf[Tool & { def jobId: Option[JobID] }]
                           .orDie
      arguments        = Array(
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
      _               <- ZIO
                           .attempt:
                             val exitCode: Int = ToolRunner.run(jobConfiguration, tool, arguments)
                             if exitCode != 0 then throw RuntimeException(s"Job submission failed with exit code $exitCode")
                           .orDie
      applicationId   <- ZIO
                           .attempt(tool.jobId.get.toString.replaceFirst("^job_", "application_"))
                           .orDie
    yield applicationId.toString

object PolygonOverlayHadoopMapReduceService:

  type Dependencies = GeoJSONSequenceService & HadoopFileSystem & HadoopConfiguration

  val layer: RLayer[Dependencies, PolygonOverlayHadoopMapReduceService] =
    ZLayer.derive[PolygonOverlayHadoopMapReduceService]

  case class ToolDescriptor(
      jar: JobArtifact,
      classFqn: String
  )
