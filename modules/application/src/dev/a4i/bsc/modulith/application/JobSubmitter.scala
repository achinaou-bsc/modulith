package dev.a4i.bsc.modulith.application

import org.geotools.data.simple.SimpleFeatureCollection
import zio.*

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
