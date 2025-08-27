package dev.a4i.bsc.modulith.application.service

import java.io.InputStream
import java.util.UUID

import org.apache.hadoop.fs.Path
import zio.*

import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.persistence.Job.*
import dev.a4i.bsc.modulith.application.persistence.JobRepository
import dev.a4i.bsc.modulith.application.postgis.GlobalAiHistoricalAverageRepository
import dev.a4i.bsc.modulith.application.postgis.WorldClimHistoricalTemperatureAverageRepository

class JobService(
    jobRepository: JobRepository,
    worldClimHistoricalTemperatureAverageRepository: WorldClimHistoricalTemperatureAverageRepository,
    globalAiHistoricalAverageRepository: GlobalAiHistoricalAverageRepository,
    hdfs: HadoopFileSystem,
    jobSubmitter: JobSubmitter,
    jobStatusSynchronizer: JobStatusSynchronizer
):

  def findAll: UIO[Vector[Persisted]] =
    jobRepository.findAll

  def findAll(status: Status): UIO[Vector[Persisted]] =
    jobRepository.findAll(status)

  def findById(id: UUID): UIO[Option[Persisted]] =
    jobRepository.findById(id)

  def submit(job: Preamble): UIO[Persisted] =
    for
      readyJob      <- jobRepository.create(job)
      base          <- worldClimHistoricalTemperatureAverageRepository.findAll(
                         readyJob.temperaturePredicate,
                         None
                       )
      overlay       <- globalAiHistoricalAverageRepository.findAll(
                         readyJob.aridityPredicate,
                         None
                       )
      computationId <- jobSubmitter.submit(readyJob, base, overlay)
      submittedJob  <- jobRepository
                         .markAsSubmitted(readyJob.id, computationId)
                         .as(readyJob.copy(status = Status.Submitted, computationId = Some(computationId)))
      _             <- jobStatusSynchronizer.watch(submittedJob)
    yield submittedJob

  def streamOutput(id: UUID): URIO[Scope, InputStream] =
    val effect: URIO[Scope & HadoopFileSystemWorkspace, InputStream] =
      for
        workspace   <- ZIO.service[HadoopFileSystemWorkspace]
        inputStream <- hdfs.open(Path(workspace.path, "output"))
      yield inputStream

    effect.provideSomeAuto(ZLayer.succeed(hdfs) >>> HadoopFileSystemWorkspace.layer(id))

  def delete(id: UUID): UIO[Unit] =
    jobRepository.delete(id)

object JobService:

  type Dependencies =
    JobRepository & WorldClimHistoricalTemperatureAverageRepository & GlobalAiHistoricalAverageRepository &
      HadoopFileSystem & JobSubmitter & JobStatusSynchronizer

  val layer: URLayer[Dependencies, JobService] =
    ZLayer.derive[JobService]
