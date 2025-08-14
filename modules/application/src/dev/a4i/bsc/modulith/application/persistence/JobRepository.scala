package dev.a4i.bsc.modulith.application.persistence

import java.util.UUID

import com.augustnagro.magnum.*
import com.augustnagro.magnum.magzio.*
import zio.*

import dev.a4i.bsc.modulith.application.persistence.Job
import dev.a4i.bsc.modulith.application.persistence.Job.*
import dev.a4i.bsc.modulith.application.persistence.JobRepository.MagnumRepository

class JobRepository(repository: MagnumRepository, transactor: TransactorZIO):

  def findAll: UIO[Vector[Persisted]] =
    transactor
      .transact(repository.findAll)
      .orDie

  def findAll(status: Status): UIO[Vector[Persisted]] =
    val specification: Spec[Persisted] =
      Spec[Persisted].where(sql"${Job.Table.status} = $status")

    transactor
      .transact(repository.findAll(specification))
      .orDie

  def findById(id: UUID): UIO[Option[Persisted]] =
    transactor
      .transact(repository.findById(id))
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
            ${Job.Table.status}        = ${Status.Submitted},
            ${Job.Table.submittedAt}   = CURRENT_TIMESTAMP
          WHERE
            ${Job.Table.id}            = ${id}
          """.update.run()
      .unit
      .orDie

  def markAsCompleted(id: UUID, status: Status): UIO[Unit] =
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

  def delete(id: UUID): UIO[Unit] =
    transactor
      .transact(repository.deleteById(id))
      .orDie

object JobRepository:

  private type MagnumRepository = Repo[Preamble, Persisted, UUID]

  val layer: URLayer[TransactorZIO, JobRepository] =
    given ZLayer.Derive.Default.WithContext[Any, Nothing, MagnumRepository] =
      ZLayer.Derive.Default.succeed(Repo[Preamble, Persisted, UUID])

    ZLayer.derive[JobRepository]
