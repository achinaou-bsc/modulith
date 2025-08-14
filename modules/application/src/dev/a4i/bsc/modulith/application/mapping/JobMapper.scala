package dev.a4i.bsc.modulith.application.mapping

import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*

import dev.a4i.bsc.modulith.application.persistence.Job
import dev.a4i.bsc.modulith.application.view.*

object JobMapper:

  given persistedToTableRow: Transformer[Job.Persisted, JobTableRow.Model] =
    _.into[JobTableRow.Model]
      .withFieldComputed(_.`type`, _.`type`.productPrefix)
      .withFieldComputed(_.status, _.status.productPrefix)
      .transform

  given creatorToPreamble: Transformer[JobCreator.Model.FormData, Job.Preamble] =
    _.into[Job.Preamble]
      .withFieldComputed(_.`type`, formData => Job.Type.valueOf(formData.`type`))
      .withFieldConst(_.status, Job.Status.Ready)
      .enableOptionDefaultsToNone
      .transform

  given persistedToViewer: Transformer[Job.Persisted, JobViewer.Model] =
    _.into[JobViewer.Model]
      .withFieldComputed(_.`type`, _.`type`.productPrefix)
      .withFieldComputed(_.status, _.status.productPrefix)
      .transform
