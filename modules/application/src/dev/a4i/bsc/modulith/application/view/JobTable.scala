package dev.a4i.bsc.modulith.application.view

import java.time.ZoneId

import zio.http.template.*

object JobTable:

  case class Model(rows: Seq[JobTableRow.Model] = Seq.empty)

  def view(model: Model)(using zoneId: ZoneId): Dom =
    div(
      classAttr := "relative overflow-x-auto",
      table(
        classAttr := "w-full text-sm text-left rtl:text-right text-gray-500 dark:text-gray-400",
        tHead(
          classAttr := "text-xs text-gray-700 uppercase bg-gray-50 dark:bg-gray-700 dark:text-gray-400",
          tr(
            th(scopeAttr := "col", classAttr := "px-6 py-3", "ID"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Type"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Status"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Computation ID"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Submitted At"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Completed At"),
            th(scopeAttr := "col", classAttr := "px-6 py-3", "Actions")
          )
        ),
        tBody(
          idAttr := "jobs",
          Html.fromSeq(model.rows.map(JobTableRow.view(_)))
        )
      )
    )
