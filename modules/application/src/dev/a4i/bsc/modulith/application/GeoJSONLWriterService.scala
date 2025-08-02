package dev.a4i.bsc.modulith.application

import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geojson.feature.FeatureJSON
import os.*
import zio.*
import zio.stream.*

import dev.a4i.bsc.modulith.application.FeatureCollectionExtensions.*

class GeoJSONLWriterService:

  def write(geoJSONLFile: Path, featureCollection: SimpleFeatureCollection): Task[Path] =
    val featureJSON: FeatureJSON = FeatureJSON()

    ZIO.scoped:
      featureCollection.featuresStream
        .map(featureJSON.toString)
        .intersperse("\n")
        .via(ZPipeline.utf8Encode)
        .run(ZSink.fromFile(geoJSONLFile.toIO))
        .as(geoJSONLFile)

object GeoJSONLWriterService:

  val layer: ULayer[GeoJSONLWriterService] =
    ZLayer.derive[GeoJSONLWriterService]
