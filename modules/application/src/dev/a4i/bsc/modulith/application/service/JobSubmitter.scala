package dev.a4i.bsc.modulith.application.service

import org.geotools.data.simple.SimpleFeatureCollection
import zio.*

import dev.a4i.bsc.modulith.application.hadoop.HadoopConfiguration
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.persistence.Job
import dev.a4i.bsc.modulith.application.persistence.JobRepository
import dev.a4i.bsc.modulith.application.service.JobStatusSynchronizer
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceGridService
import dev.a4i.bsc.modulith.application.service.PolygonOverlayHadoopMapReduceNaiveService

class JobSubmitter(
    polygonOverlayMapReduceNaiveService: PolygonOverlayHadoopMapReduceNaiveService,
    polygonOverlayMapReduceGridService: PolygonOverlayHadoopMapReduceGridService,
    jobRepository: JobRepository,
    jobStatusSynchronizer: JobStatusSynchronizer
):

  def submit(
      job: Job.Persisted,
      base: SimpleFeatureCollection,
      overlay: SimpleFeatureCollection
  ): URIO[HadoopFileSystem & HadoopConfiguration, Unit] =
    for
      computationId <- job.`type` match
                         case Job.Type.Naive =>
                           polygonOverlayMapReduceNaiveService
                             .submit(
                               job.id,
                               base,
                               overlay
                             )
                             .provideSomeAuto(HadoopFileSystemWorkspace.layer(job.id))
                         case Job.Type.Grid  =>
                           polygonOverlayMapReduceGridService
                             .submit(
                               job.id,
                               base,
                               overlay
                             )
                             .provideSomeAuto(HadoopFileSystemWorkspace.layer(job.id))
      _             <- jobRepository
                         .markAsSubmitted(job.id, computationId)
                         .as(job.copy(computationId = Some(computationId)))
                         .flatMap(jobStatusSynchronizer.watch)
    yield ()

object JobSubmitter:

  type Dependencies =
    PolygonOverlayHadoopMapReduceNaiveService & PolygonOverlayHadoopMapReduceGridService & JobRepository &
      JobStatusSynchronizer

  val layer: URLayer[Dependencies, JobSubmitter] =
    ZLayer.derive[JobSubmitter]
