package dev.a4i.bsc.modulith.application.view

import zio.http.htmx.*
import zio.http.template.*
import zio.schema.*

object JobCreator:

  case class Model(formData: Option[Model.FormData] = None)

  object Model:

    case class FormData(
        `type`: String,
        temperaturePredicate: String,
        aridityPredicate: String
    )

    object FormData:

      given Schema[FormData] = DeriveSchema.gen

  def view(model: Model): Dom =
    div(
      form(
        hxExtAttr  := "form-json",
        hxPostAttr := "/jobs/creator",
        div(
          classAttr := "grid gap-6 mb-6 md:grid-cols-1",
          div(
            label(
              forAttr      := "type",
              classAttr    := "block mb-2 text-sm font-medium text-gray-900 dark:text-white",
              "Type"
            ),
            select(
              idAttr       := "type",
              nameAttr     := "type",
              requiredAttr := true,
              classAttr    := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500",
              option(
                valueAttr    := "Naive",
                selectedAttr := "selected",
                "Naive"
              ),
              option(
                valueAttr    := "Grid",
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
              requiredAttr    := true,
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
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
              requiredAttr    := true,
              classAttr       := "bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5 dark:bg-gray-700 dark:border-gray-600 dark:placeholder-gray-400 dark:text-white dark:focus:ring-blue-500 dark:focus:border-blue-500"
            )
          )
        ),
        button(
          typeAttr  := "submit",
          classAttr := "text-gray-900 bg-white border border-gray-300 focus:outline-none hover:bg-gray-100 focus:ring-4 focus:ring-gray-100 font-medium rounded-lg text-sm px-5 py-2.5 me-2 mb-2 dark:bg-gray-800 dark:text-white dark:border-gray-600 dark:hover:bg-gray-700 dark:hover:border-gray-600 dark:focus:ring-gray-700",
          "Submit Job"
        )
      )
    )
