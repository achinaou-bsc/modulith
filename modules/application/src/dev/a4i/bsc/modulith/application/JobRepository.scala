package dev.a4i.bsc.modulith.application

import java.util.UUID

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

  def create(job: Preamble): UIO[Persisted] =
    transactor
      .transact(repository.insertReturning(job))
      .orDie

  def markAsSubmitted(id: UUID, computationId: String): UIO[Unit] =
    transactor
      .transact:
        sql"""
          UPDATE ${Job.Table}
          SET
            ${Job.Table.computationId} = ${computationId},
            ${Job.Table.status}        = ${Job.Status.Submitted},
            ${Job.Table.submittedAt}   = CURRENT_TIMESTAMP
          WHERE
            ${Job.Table.id}            = ${id}
          """.update.run()
      .unit
      .orDie

  def markAsCompleted(id: UUID, status: Status) =
    transactor
      .transact:
        sql"""
          UPDATE ${Job.Table}
          SET
            ${Job.Table.status}      = ${status},
            ${Job.Table.completedAt} = CURRENT_TIMESTAMP
          WHERE
            ${Job.Table.id}          = ${id}
          """.update.run()
      .unit
      .orDie

object JobRepository:

  private type MagnumRepository = Repo[Preamble, Persisted, UUID]

  val layer: URLayer[TransactorZIO, JobRepository] =
    given ZLayer.Derive.Default.WithContext[Any, Nothing, MagnumRepository] =
      ZLayer.Derive.Default.succeed(Repo[Preamble, Persisted, UUID])

    ZLayer.derive[JobRepository]
