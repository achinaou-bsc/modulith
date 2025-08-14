package dev.a4i.bsc.modulith.application.service

import java.util.UUID

import zio.*

import dev.a4i.bsc.modulith.application.persistence.Job.*
import dev.a4i.bsc.modulith.application.persistence.JobRepository

class JobService(jobRepository: JobRepository):

  def findAll: UIO[Vector[Persisted]] =
    jobRepository.findAll

  def findAll(status: Status): UIO[Vector[Persisted]] =
    jobRepository.findAll(status)

  def findById(id: UUID): UIO[Option[Persisted]] =
    jobRepository.findById(id)

  def create(job: Preamble): UIO[Persisted] =
    jobRepository.create(job)

  def markAsSubmitted(id: UUID, computationId: String): UIO[Unit] =
    jobRepository.markAsSubmitted(id, computationId)

  def markAsCompleted(id: UUID, status: Status): UIO[Unit] =
    jobRepository.markAsCompleted(id, status)

  def delete(id: UUID): UIO[Unit] =
    jobRepository.delete(id)

object JobService:

  val layer: URLayer[JobRepository, JobService] =
    ZLayer.derive[JobService]
