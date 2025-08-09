package dev.a4i.bsc.modulith.application

import java.time.Instant

import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.*
import zio.*

import dev.a4i.bsc.modulith.application.Job.*
import dev.a4i.bsc.modulith.application.JobRepository.MagnumRepository

class JobRepository(repository: MagnumRepository, transactor: TransactorZIO):

  def findAll(status: Status): UIO[Vector[Persisted]] =
    val specification: Spec[Persisted] =
      Spec[Persisted].where(sql"${Job.Table.status} = $status")

    transactor
      .transact(repository.findAll(specification))
      .orDie

  def create(job: Preamble): UIO[String] =
    transactor
      .transact(repository.insertReturning(job).id)
      .orDie

  def markAsSubmitted(id: String, computationId: String): UIO[Unit] =
    transactor
      .transact:
        sql"""
          UPDATE ${Job.Table}
          SET
            ${Job.Table.computationId} = ${computationId},
            ${Job.Table.status}        = ${Job.Status.Submitted}
            ${Job.Table.submittedAt}   = ${Instant.now}
          WHERE
            ${Job.Table.id}            = ${id}
          """.update.run()
      .unit
      .orDie

  def markAsSucceeded(id: String): UIO[Unit] =
    markAsCompleted(id, Job.Status.Succeeded)

  def markAsFailed(id: String): UIO[Unit] =
    markAsCompleted(id, Job.Status.Failed)

  def markAsCancelled(id: String): UIO[Unit] =
    markAsCompleted(id, Job.Status.Cancelled)

  def markAsCompleted(id: String): UIO[Unit] =
    markAsCompleted(id, Job.Status.Completed)

  private def markAsCompleted(id: String, status: Status) =
    transactor
      .transact:
        sql"""
              UPDATE ${Job.Table}
              SET
                ${Job.Table.status}      = ${status},
                ${Job.Table.completedAt} = ${Instant.now}
              WHERE
                ${Job.Table.id}          = ${id}
              """.update.run()
      .unit
      .orDie

object JobRepository:

  private type MagnumRepository = Repo[Preamble, Persisted, String]

  val layer: URLayer[TransactorZIO, JobRepository] =
    given ZLayer.Derive.Default.WithContext[Any, Nothing, MagnumRepository] =
      ZLayer.Derive.Default.succeed(Repo[Preamble, Persisted, String])

    ZLayer.derive[JobRepository]
