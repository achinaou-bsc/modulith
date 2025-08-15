package dev.a4i.bsc.modulith.application.service

import org.geotools.data.simple.SimpleFeatureCollection
import zio.*

import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.persistence.Job
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceGridService
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceNaiveService

class JobSubmitter(
    polygonOverlayMapReduceNaiveService: PolygonOverlayHadoopMapReduceNaiveService,
    polygonOverlayMapReduceGridService: PolygonOverlayHadoopMapReduceGridService,
    hdfs: HadoopFileSystem
):

  def submit(job: Job.Persisted, base: SimpleFeatureCollection, overlay: SimpleFeatureCollection): UIO[String] =
    job.`type` match
      case Job.Type.Naive =>
        polygonOverlayMapReduceNaiveService
          .submit(
            job.id,
            base,
            overlay
          )
          .provideSomeAuto(ZLayer.succeed(hdfs) >>> HadoopFileSystemWorkspace.layer(job.id))
      case Job.Type.Grid  =>
        polygonOverlayMapReduceGridService
          .submit(
            job.id,
            base,
            overlay
          )
          .provideSomeAuto(ZLayer.succeed(hdfs) >>> HadoopFileSystemWorkspace.layer(job.id))

object JobSubmitter:

  type Dependencies =
    PolygonOverlayHadoopMapReduceNaiveService & PolygonOverlayHadoopMapReduceGridService & HadoopFileSystem

  val layer: URLayer[Dependencies, JobSubmitter] =
    ZLayer.derive[JobSubmitter]
