package dev.a4i.bsc.modulith.application.view

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

import zio.http.template.*

object JobViewer:

  case class Model(
      id: UUID,
      `type`: String,
      status: String,
      computationId: Option[String],
      submittedAt: Option[Instant],
      completedAt: Option[Instant]
  )

  def view(model: Model)(using zoneId: ZoneId): Dom =
    ???
