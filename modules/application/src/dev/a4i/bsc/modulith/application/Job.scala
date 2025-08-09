package dev.a4i.bsc.modulith.application

import java.time.Instant
import java.util.UUID

import com.augustnagro.magnum.*

import dev.a4i.bsc.modulith.application.Job.*

enum Job:

  def `type`: Type
  def status: Status
  def computationId: Option[String]

  case Preamble(
      `type`: Type,
      status: Status,
      computationId: Option[String] = None
  )

  @SqlName("jobs") @Table(PostgresDbType, SqlNameMapper.CamelToSnakeCase) case Persisted(
      @Id id: UUID,
      `type`: Type,
      status: Status,
      computationId: Option[String],
      submittedAt: Option[Instant],
      completedAt: Option[Instant]
  )

object Job:

  given DbCodec[Job.Preamble]  = DbCodec.derived
  given DbCodec[Job.Persisted] = DbCodec.derived

  val Table = TableInfo[Job.Preamble, Job.Persisted, UUID]

  @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
  enum Type derives CanEqual, DbCodec:
    case Naive

  @Table(PostgresDbType, SqlNameMapper.CamelToUpperSnakeCase)
  enum Status derives CanEqual, DbCodec:
    case Ready
    case Submitted
    case Succeeded
    case Failed
    case Cancelled
    case Ended
