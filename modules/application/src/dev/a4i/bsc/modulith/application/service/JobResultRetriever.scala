package dev.a4i.bsc.modulith.application.service

import java.util.UUID

import org.geotools.data.simple.SimpleFeatureCollection
import zio.*

import dev.a4i.bsc.modulith.application.hadoop.HadoopFileSystem

class JobResultRetriever(hadoopFileSystem: HadoopFileSystem):

  def retrieve(jobId: UUID): UIO[SimpleFeatureCollection] =
    ???

object JobResultRetriever:

  val layer: URLayer[HadoopFileSystem, JobResultRetriever] =
    ZLayer.derive[JobResultRetriever]
