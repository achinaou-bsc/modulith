package dev.a4i.bsc.modulith.application.htmx

import java.util.UUID

import zio.http.Header
import zio.http.htmx.*
import zio.http.template.*
import zio.http.template.Attributes.PartialAttribute
import zio.http.template.Element.PartialElement

def hxTriggerHeader(event: Event, events: Event*): Header =
  Header.Custom(
    "HX-Trigger",
    Array(event)
      .appendedAll(events)
      .map(_.name)
      .mkString(", ")
  )

def hxPushUrlHeader(url: String): Header =
  Header.Custom("HX-Push", url)

def hxOnAttr(event: String) = PartialAttribute[String](s"hx-on:$event")

def hxSwapOutOfBand(vehicle: PartialElement, swapOutOfBand: String, content: Html): Html =
  vehicle(hxSwapOobAttr := swapOutOfBand, content)

enum Event(val name: String):
  case JobCreated(id: UUID) extends Event(s"application:job-$id-created")
  case JobUpdated(id: UUID) extends Event(s"application:job-$id-updated")
  case JobDeleted(id: UUID) extends Event(s"application:job-$id-deleted")
