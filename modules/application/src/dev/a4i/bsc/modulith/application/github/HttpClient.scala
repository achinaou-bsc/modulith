package dev.a4i.bsc.modulith.application.github

import zio.*
import zio.http.*
import zio.http.netty.NettyConfig

type HttpClient = zio.http.Client

object HttpClient:

  val layer: ULayer[HttpClient] =
    val followRedirectsAspect: ZClientAspect[Nothing, Any, Nothing, Body, Nothing, Any, Nothing, Response] =
      ZClientAspect.followRedirects(1)((response, message) => ZIO.logInfo(message).as(response))

    ZLayer
      .make[HttpClient](
        ZLayer.succeed(ZClient.Config.default.idleTimeout(5.minutes).disabledConnectionPool),
        ZLayer.succeed(NettyConfig.default),
        DnsResolver.default,
        ZClient.live
      )
      .map(_.update(_ @@ followRedirectsAspect))
      .orDie
