package dev.a4i.bsc.modulith.application.service

import java.io.InputStream
import java.util.UUID

import org.apache.hadoop.fs.Path
import zio.*

import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem
import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystemWorkspace
import dev.a4i.bsc.modulith.application.persistence.Job.*
import dev.a4i.bsc.modulith.application.persistence.JobRepository
import dev.a4i.bsc.modulith.application.postgis.WADAridityRepository
import dev.a4i.bsc.modulith.application.postgis.WorldClimHistoricalTemperatureAverageRepository

class JobService(
    jobRepository: JobRepository,
    worldClimHistoricalTemperatureAverageRepository: WorldClimHistoricalTemperatureAverageRepository,
    wadAridityRepository: WADAridityRepository,
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
                         Some(1)
                       )
      overlay       <- wadAridityRepository.findAll(
                         readyJob.aridityPredicate,
                         Some(1)
                       )
      computationId <- jobSubmitter.submit(readyJob, base, base)
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
    JobRepository & WorldClimHistoricalTemperatureAverageRepository & WADAridityRepository & HadoopFileSystem &
      JobSubmitter & JobStatusSynchronizer

  val layer: URLayer[Dependencies, JobService] =
    ZLayer.derive[JobService]
