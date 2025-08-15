package dev.a4i.bsc.modulith.application.service

import java.util.UUID

import JobArtifactManager.JobArtifact
import org.geotools.data.simple.SimpleFeatureCollection
import zio.*
import zio.config.*
import zio.config.magnolia.deriveConfig

import dev.a4i.bsc.modulith.application.github.GitHub
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceService.ToolDescriptor

class PolygonOverlayHadoopMapReduceGridService(
    jobArtifactManager: JobArtifactManager,
    polygonOverlayHadoopMapReduceService: PolygonOverlayHadoopMapReduceService,
    configuration: PolygonOverlayHadoopMapReduceGridService.Configuration
):

  def submit(
      jobId: UUID,
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystemWorkspace, String] =
    for
      toolDescriptor    <- getToolDescriptor
      yarnApplicationId <- polygonOverlayHadoopMapReduceService.submit(
                             jobId,
                             toolDescriptor,
                             base,
                             overlay
                           )
    yield yarnApplicationId

  def getToolDescriptor: UIO[ToolDescriptor] =
    jobArtifactManager
      .get(GitHub.AssetQuery(
        configuration.token,
        configuration.owner,
        configuration.repository,
        configuration.tag,
        configuration.jarName
      ))
      .zip(ZIO.succeed(
        configuration.toolClassFqn
      ))
      .map(ToolDescriptor.apply)

object PolygonOverlayHadoopMapReduceGridService:

  type Dependencies = JobArtifactManager & PolygonOverlayHadoopMapReduceService

  val layer: RLayer[Dependencies, PolygonOverlayHadoopMapReduceGridService] =
    ZLayer.derive[PolygonOverlayHadoopMapReduceGridService]

  case class Configuration(
      token: String,
      owner: String,
      repository: String,
      tag: String,
      jarName: String,
      toolClassFqn: String
  )

  object Configuration:

    given Config[Configuration] =
      deriveConfig[Configuration].nested("polygon", "overlay", "hadoop", "mapreduce", "grid").mapKey(toKebabCase)
