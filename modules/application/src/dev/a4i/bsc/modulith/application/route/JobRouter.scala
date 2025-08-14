package dev.a4i.bsc.modulith.application.route

import java.time.ZoneId
import java.util.UUID

import io.scalaland.chimney.dsl.*
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.literal
import zio.http.template.*
import zio.schema.Schema
import zio.schema.codec.BinaryCodec
import zio.schema.codec.JsonCodec.schemaBasedBinaryCodec

import dev.a4i.bsc.modulith.application.htmx.*
import dev.a4i.bsc.modulith.application.mapping.JobMapper.given
import dev.a4i.bsc.modulith.application.persistence.Job
import dev.a4i.bsc.modulith.application.service.JobService
import dev.a4i.bsc.modulith.application.view.*

class JobRouter(jobService: JobService):

  val routes: Routes[Any, Response] =
    literal("jobs") / (tableRoutes ++ creatorRoutes ++ viewerRoutes)

  private lazy val tableRoutes: Routes[Any, Response] =
    literal("table") / Routes(
      Method.GET / Root          -> handler: (request: Request) =>
        given ZoneId = ZoneId.of(request.header[String]("X-Timezone").getOrElse("UTC"))

        for
          entities <- jobService.findAll
          models   <- ZIO
                        .fromEither(entities.transformIntoPartial[Seq[JobTableRow.Model]].asEither)
                        .orDieWith(errors => RuntimeException(errors.errors.map(_.toString).mkString(", ")))
          table     = JobTable.view(JobTable.Model(models))
          response  = Response.html(table)
        yield response
      ,
      Method.GET / uuid("id")    -> handler: (id: UUID, request: Request) =>
        given ZoneId = ZoneId.of(request.header[String]("X-Timezone").getOrElse("UTC"))

        for
          entity  <- jobService.findById(id)
          model   <- ZIO
                       .fromEither(entity.transformIntoPartial[Option[JobTableRow.Model]].asEither)
                       .orDieWith(errors => RuntimeException(errors.errors.map(_.toString).mkString(", ")))
          row      = model.map(model => JobTableRow.view(model))
          response = row.fold(Response.notFound)(Response.html(_))
        yield response
      ,
      Method.DELETE / uuid("id") -> handler: (id: UUID, _: Request) =>
        for
          _       <- jobService.delete(id)
          response = Response.ok.addHeader(hxTriggerHeader(Event.JobDeleted(id)))
        yield response
    )

  private lazy val creatorRoutes: Routes[Any, Response] =
    literal("creator") / Routes(
      Method.GET / Root  -> Handler.fromResponse(Response.html(JobCreator.view(JobCreator.Model()))),
      Method.POST / Root -> handler: (request: Request) =>
        given ZoneId = ZoneId.of(request.header[String]("X-Timezone").getOrElse("UTC"))

        for
          body          <- request.body.asChunk.orDie
          entity        <- ZIO
                             .fromEither(summon[BinaryCodec[JobCreator.Model.FormData]].decode(body))
                             .map(_.transformInto[Job.Preamble])
                             .orDieWith(RuntimeException(_))
          createdEntity <- jobService.create(entity)
          model          = createdEntity.transformInto[JobViewer.Model]
          viewer         = JobViewer.view(model)
          response       = Response
                             .html(viewer)
                             .addHeader(hxTriggerHeader(Event.JobCreated(model.id)))
        yield response
    )

  private lazy val viewerRoutes: Routes[Any, Response] =
    literal("viewer") / Routes(
      Method.GET / Root / uuid("id") -> handler: (id: UUID, request: Request) =>
        given ZoneId = ZoneId.of(request.header[String]("X-Timezone").getOrElse("UTC"))

        for
          entity  <- jobService.findById(id)
          model   <- ZIO
                       .fromEither(entity.transformIntoPartial[Option[JobViewer.Model]].asEither)
                       .orDieWith(errors => RuntimeException(errors.errors.map(_.toString).mkString(", ")))
          viewer   = model.map(JobViewer.view)
          response = viewer.fold(Response.notFound)(Response.html(_))
        yield response
    )

object JobRouter:

  val layer: URLayer[JobService, JobRouter] =
    ZLayer.derive[JobRouter]
