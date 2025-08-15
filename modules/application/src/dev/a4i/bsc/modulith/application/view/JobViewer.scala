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
      temperaturePredicate: String,
      aridityPredicate: String,
      computationId: Option[String],
      submittedAt: Option[Instant],
      completedAt: Option[Instant]
  )

  def view(model: Model)(using zoneId: ZoneId): Dom =
    div(
      form(
        div(
          classAttr := "grid gap-6 mb-6 md:grid-cols-2",
          div(
            label(
              forAttr         := "id",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "ID"
            ),
            input(
              idAttr          := "id",
              nameAttr        := "id",
              placeholderAttr := "ID",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.id.toString
            )
          ),
          div(
            label(
              forAttr      := "type",
              classAttr    := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Type"
            ),
            select(
              idAttr       := "type",
              nameAttr     := "type",
              disabledAttr := "disabled",
              classAttr    := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              option(
                valueAttr := "Naive",
                model.`type` match
                  case "Naive" => selectedAttr := "selected"
                  case _       => Dom.empty,
                "Naive"
              ),
              option(
                valueAttr := "Grid",
                model.`type` match
                  case "Grid" => selectedAttr := "selected"
                  case _      => Dom.empty,
                "Grid"
              )
            )
          ),
          div(
            label(
              forAttr         := "temperaturePredicate",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Temperature Predicate"
            ),
            input(
              idAttr          := "temperaturePredicate",
              nameAttr        := "temperaturePredicate",
              placeholderAttr := "Temperature Predicate",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.temperaturePredicate
            )
          ),
          div(
            label(
              forAttr         := "aridityPredicate",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Aridity Predicate"
            ),
            input(
              idAttr          := "aridityPredicate",
              nameAttr        := "aridityPredicate",
              placeholderAttr := "Aridity Predicate",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.aridityPredicate
            )
          ),
          div(
            label(
              forAttr         := "computationId",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Computation Id"
            ),
            input(
              idAttr          := "computationId",
              nameAttr        := "computationId",
              placeholderAttr := "Computation Id",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.computationId.getOrElse("")
            )
          ),
          div(
            label(
              forAttr         := "submittedAt",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Submitted At"
            ),
            input(
              idAttr          := "submittedAt",
              nameAttr        := "submittedAt",
              placeholderAttr := "Submitted At",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.submittedAt.fold("")(dateTimeFormatter.withZone(zoneId).format)
            )
          ),
          div(
            label(
              forAttr         := "completedAt",
              classAttr       := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Completed At"
            ),
            input(
              idAttr          := "completedAt",
              nameAttr        := "completedAt",
              placeholderAttr := "Completed At",
              disabledAttr    := "disabled",
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              valueAttr       := model.completedAt.fold("")(dateTimeFormatter.withZone(zoneId).format)
            )
          )
        )
      ),
      div(idAttr      := "map"),
      script(typeAttr := "module", srcAttr := "/assets/scripts/map.js")
    )
