package dev.a4i.bsc.modulith.application.persistence

import java.time.Instant
import java.util.UUID

import com.augustnagro.magnum.*
import zio.schema.DeriveSchema
import zio.schema.Schema

import dev.a4i.bsc.modulith.application.persistence.Job.*

enum Job:

  def `type`: Type
  def status: Status
  def temperaturePredicate: String
  def aridityPredicate: String

  case Preamble(
      `type`: Type,
      status: Status,
      temperaturePredicate: String,
      aridityPredicate: String
  )

  @SqlName("jobs") @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase) case Persisted(
      @Id id: UUID,
      `type`: Type,
      status: Status,
      temperaturePredicate: String,
      aridityPredicate: String,
      computationId: Option[String],
      submittedAt: Option[Instant],
      completedAt: Option[Instant]
  )

object Job:

  given Schema[Job]           = DeriveSchema.gen
  given Schema[Job.Preamble]  = DeriveSchema.gen
  given Schema[Job.Persisted] = DeriveSchema.gen

  given DbCodec[Job.Preamble]  = DbCodec.derived
  given DbCodec[Job.Persisted] = DbCodec.derived

  val Table = TableInfo[Job.Preamble, Job.Persisted, UUID]

  @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
  enum Type derives CanEqual, DbCodec:
    case Naive
    case Grid

  @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
  enum Status derives CanEqual, DbCodec:
    case Ready
    case Submitted
    case Succeeded
    case Failed
    case Cancelled
    case Ended
