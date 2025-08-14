package dev.a4i.bsc.modulith.application.route

import zio.*
import zio.http.*

class StaticRouter:

  val routes: Routes[Any, Nothing] =
    Routes.empty @@ Middleware.serveResources(Path.empty / "assets")

object StaticRouter:

  val layer: ULayer[StaticRouter] =
    ZLayer.derive[StaticRouter]
