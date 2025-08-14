package dev.a4i.bsc.modulith.application.route

import zio.*
import zio.http.*

class DynamicRouter(jobRouter: JobRouter):

  val routes: Routes[Any, Response] =
    jobRouter.routes

object DynamicRouter:

  val layer: URLayer[JobRouter, DynamicRouter] =
    ZLayer.derive[DynamicRouter]
