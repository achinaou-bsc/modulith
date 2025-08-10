package dev.a4i.bsc.modulith.application

import java.util.UUID
import scala.annotation.nowarn

import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus
import org.apache.hadoop.yarn.client.api.YarnClient
import zio.*
import zio.config.magnolia.deriveConfig
import zio.stm.*

import dev.a4i.bsc.modulith.application.Job.Status
import dev.a4i.bsc.modulith.application.JobStatusSynchronizer.Configuration

class JobStatusSynchronizer(
    jobRepository: JobRepository,
    yarnClient: YarnClient,
    fibers: TMap[UUID, Fiber.Runtime[Nothing, Unit]],
    scope: Scope,
    configuration: Configuration
):

  @nowarn("msg=unused") private given CanEqual[FinalApplicationStatus, FinalApplicationStatus] = CanEqual.derived

  def watch(job: Job.Persisted): UIO[Unit] =
    for
      gate  <- Promise.make[Nothing, Unit]
      fiber <- (gate.await *> synchronize(job))
                 .ensuring(fibers.delete(job.id).commit)
                 .logError
                 .forkIn(scope)
      added <- ZSTM.atomically:
                 for
                   alreadyBeingWatched <- fibers.contains(job.id)
                   added               <- if alreadyBeingWatched
                                          then ZSTM.succeed(false)
                                          else fibers.put(job.id, fiber).as(true)
                 yield added
      _     <- if added
               then gate.succeed(())
               else fiber.interrupt
    yield ()

  private def synchronize(job: Job.Persisted): UIO[Unit] =
    fetchJobStatus(job)
      .tap(status => ZIO.log(s"Job ${job.id} is $status"))
      .logError
      .repeat(schedule)
      .flatMap(handleCompletion(job, _))

  private def fetchJobStatus(job: Job.Persisted): UIO[Status] =
    for
      applicationId        <- ZIO.attempt(ApplicationId.fromString(job.computationId.get)).orDie
      report               <- ZIO.attemptBlocking(yarnClient.getApplicationReport(applicationId)).orDie
      applicationFinalState = report.getFinalApplicationStatus
    yield applicationFinalState match
      case FinalApplicationStatus.UNDEFINED => Status.Submitted
      case FinalApplicationStatus.SUCCEEDED => Status.Succeeded
      case FinalApplicationStatus.FAILED    => Status.Failed
      case FinalApplicationStatus.KILLED    => Status.Cancelled
      case FinalApplicationStatus.ENDED     => Status.Ended

  private def schedule: Schedule[Any, Status, Status] =
    Schedule.spaced(configuration.interval) *> Schedule.recurWhile: status =>
      status == Status.Ready || status == Status.Submitted

  private def handleCompletion(job: Job.Persisted, status: Status): UIO[Unit] =
    status match
      case Status.Succeeded | Status.Failed | Status.Cancelled | Status.Ended =>
        jobRepository.markAsCompleted(job.id, status)
      case Status.Ready | Status.Submitted                                    =>
        ZIO.unit

object JobStatusSynchronizer:

  val layer: URLayer[JobRepository & YarnClient, JobStatusSynchronizer] =
    ZLayer.scoped:
      ZIO.scopeWith: scope =>
        for
          jobRepository <- ZIO.service[JobRepository]
          yarnClient    <- ZIO.service[YarnClient]
          fibers        <- TMap.empty[UUID, Fiber.Runtime[Nothing, Unit]].commit
          configuration <- ZIO.config[Configuration].orDie
        yield JobStatusSynchronizer(jobRepository, yarnClient, fibers, scope, configuration)

  case class Configuration(
      interval: Duration
  )

  object Configuration:

    given Config[Configuration] = deriveConfig[Configuration].nested("job", "status", "synchronizer")
