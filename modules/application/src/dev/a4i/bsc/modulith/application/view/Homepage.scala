package dev.a4i.bsc.modulith.application.view

import zio.http.htmx.*
import zio.http.template.*

object Homepage:

  def view(mainPage: Html = Html.fromDomElement(Dom.empty)): Html =
    html(
      head(
        meta(charsetAttr := "UTF-8"),
        meta(nameAttr    := "viewport", contentAttr := "width=device-width, initial-scale=1.0"),
        title("Polygon Overlay"),
        script(srcAttr   := "/assets/scripts/tailwindcss.min.js"),
        style(typeAttr   := "text/tailwindcss"),
        link(relAttr     := "stylesheet", hrefAttr  := "/assets/styles/flowbite.min.css"),
        link(relAttr     := "stylesheet", hrefAttr  := "https://esm.sh/ol/ol.css"),
        link(relAttr     := "stylesheet", hrefAttr  := "/assets/styles/main.css"),
        script(srcAttr   := "/assets/scripts/flowbite.min.js"),
        script(srcAttr   := "/assets/scripts/htmx.min.js"),
        script(srcAttr   := "/assets/scripts/htmx-form-json.js"),
        script(typeAttr  := "module", srcAttr       := "/assets/scripts/app.js")
      ),
      body(
        hxHeadersAttr := """js:{"X-Timezone": Intl.DateTimeFormat().resolvedOptions().timeZone}""",
        classAttr     := "bg-gray-50 dark:bg-gray-900",
        nav(
          classAttr     := "bg-white border-gray-200 dark:bg-gray-900",
          div(
            classAttr := "flex flex-wrap items-center justify-between mx-auto p-4",
            a(
              hrefAttr  := "http://localhost:8080",
              classAttr := "flex items-center space-x-3 rtl:space-x-reverse",
              img(
                srcAttr   := "/assets/images/logo.png",
                altAttr   := "Logo",
                classAttr := "h-8"
              ),
              span(
                classAttr := "self-center text-2xl font-semibold whitespace-nowrap dark:text-white",
                "Polygon Overlay"
              )
            ),
            div(
              classAttr := "flex items-center md:order-2 space-x-1 md:space-x-2 rtl:space-x-reverse",
              button(
                typeAttr     := "button",
                classAttr    := "text-gray-900 bg-white border border-gray-300 focus:outline-none hover:bg-gray-100 focus:ring-4 focus:ring-gray-100 font-medium rounded-lg text-sm px-5 py-2.5 me-2 mb-2 dark:bg-gray-800 dark:text-white dark:border-gray-600 dark:hover:bg-gray-700 dark:hover:border-gray-600 dark:focus:ring-gray-700",
                hxGetAttr    := "/jobs/creator",
                hxTargetAttr := "#main",
                "New Job"
              )
            )
          )
        ),
        main(
          idAttr        := "main",
          hxGetAttr     := "/jobs/table",
          hxTriggerAttr := "load",
          classAttr     := "bg-white border-gray-200 dark:bg-gray-900 p-4",
          mainPage
        )
      )
    )
