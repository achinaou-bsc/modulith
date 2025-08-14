package dev.a4i.bsc.modulith.application.route

import zio.*
import zio.http.*

import dev.a4i.bsc.modulith.application.view.Homepage

class Router(staticRoutes: StaticRouter, dynamicRoutes: DynamicRouter):

  val routes: Routes[Any, Response] =
    Routes(Method.GET / Root -> Handler.html(Homepage.view()))
      ++ staticRoutes.routes
      ++ dynamicRoutes.routes

object Router:

  val layer: URLayer[StaticRouter & DynamicRouter, Router] =
    ZLayer.derive[Router]
