package dev.a4i.bsc.modulith.application.view

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

import zio.http.htmx.*
import zio.http.template.*

object JobTableRow:

  case class Model(
      id: UUID,
      `type`: String,
      status: String,
      computationId: Option[String],
      submittedAt: Option[Instant],
      completedAt: Option[Instant]
  )

  def view(model: Model)(using zoneId: ZoneId): Dom =
    tr(
      classAttr := "bg-white border-b dark:bg-gray-800 dark:border-gray-700 border-gray-200",
      th(
        scopeAttr := "row",
        classAttr := "px-6 py-4 font-medium text-gray-900 whitespace-nowrap dark:text-white",
        model.id.toString
      ),
      td(
        classAttr := "px-6 py-4",
        model.`type`.toString
      ),
      td(
        classAttr := "px-6 py-4",
        model.status.toString
      ),
      td(
        classAttr := "px-6 py-4",
        model.computationId.getOrElse("").toString
      ),
      td(
        classAttr := "px-6 py-4",
        model.submittedAt.fold("")(dateTimeFormatter.withZone(zoneId).format)
      ),
      td(
        classAttr := "px-6 py-4",
        model.completedAt.fold("")(dateTimeFormatter.withZone(zoneId).format)
      ),
      td(
        classAttr := "px-6 py-4",
        button(
          typeAttr      := "button",
          hxDeleteAttr  := s"/fragments/jobs/${model.id}",
          hxTargetAttr  := "#main",
          hxConfirmAttr := s"Delete job ${model.id}?",
          classAttr     := "focus:outline-none text-white bg-red-700 hover:bg-red-800 focus:ring-4 focus:ring-red-300 font-medium rounded-lg text-sm px-5 py-2.5 me-2 mb-2 dark:bg-red-600 dark:hover:bg-red-700 dark:focus:ring-red-900",
          "Delete"
        )
      )
    )
