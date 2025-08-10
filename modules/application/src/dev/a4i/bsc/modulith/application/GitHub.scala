package dev.a4i.bsc.modulith.application

import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json
import zio.json.ast.JsonCursor
import zio.stream.*

import dev.a4i.bsc.modulith.application.GitHub.AssetQuery

class GitHub(client: Client):

  def streamReleaseAsset(assetQuery: AssetQuery): ZStream[Scope, Nothing, Byte] =
    val assetRequestZIO: URIO[Scope, Request] =
      for
        releaseRequest       = Request
                                 .get(releaseRequestURL(assetQuery))
                                 .addHeader(Header.Authorization.Bearer(assetQuery.token))
                                 .addHeader(Header.Accept(MediaType.application.json))
        releaseResponse     <- client.request(releaseRequest).orDie
        releaseResponseBody <- releaseResponse.body.asString.orDie
        releaseResponseJson <- ZIO
                                 .fromEither(releaseResponseBody.fromJson[Json])
                                 .orDieWith(RuntimeException(_))
        assetId             <- ZIO
                                 .fromEither:
                                   for
                                     assetsJson <- releaseResponseJson.get(JsonCursor.field("assets").isArray)
                                     assetId    <- assetsJson.elements
                                                     .find(_.get(JsonCursor.field("name").isString).exists(_.value == assetQuery.name))
                                                     .match
                                                       case Some(asset) =>
                                                         asset
                                                           .get(JsonCursor.field("id").isNumber)
                                                           .map(_.value.longValue)
                                                       case None        =>
                                                         Left(s"Asset '${assetQuery.name}' not found in release '${assetQuery.tag}'")
                                   yield assetId
                                 .orDieWith(RuntimeException(_))
        assetRequest         = Request
                                 .get(assetRequestURL(assetQuery, assetId))
                                 .addHeader(Header.Authorization.Bearer(assetQuery.token))
                                 .addHeader(Header.Accept(MediaType.application.`octet-stream`))
      yield assetRequest

    ZStream
      .fromZIO(assetRequestZIO)
      .flatMap(client.stream(_)(_.body.asStream))
      .orDie

  private def releaseRequestURL(assetQuery: AssetQuery): String =
    s"https://api.github.com/repos/${assetQuery.owner}/${assetQuery.repository}/releases/tags/${assetQuery.tag}"

  private def assetRequestURL(assetQuery: AssetQuery, assetId: Long): String =
    s"https://api.github.com/repos/${assetQuery.owner}/${assetQuery.repository}/releases/assets/$assetId"

object GitHub:

  val layer: URLayer[Client, GitHub] =
    ZLayer.derive[GitHub]

  case class AssetQuery(
      token: String,
      owner: String,
      repository: String,
      tag: String,
      name: String
  )
