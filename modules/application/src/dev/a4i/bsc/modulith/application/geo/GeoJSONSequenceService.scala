package dev.a4i.bsc.modulith.application.geo

import org.geotools.data.geojson.GeoJSONWriter
import org.geotools.data.simple.SimpleFeatureCollection
import zio.*
import zio.stream.*

import dev.a4i.bsc.modulith.application.geo.FeatureCollectionExtensions.*

class GeoJSONSequenceService:

  def encode(featureCollection: SimpleFeatureCollection): ZStream[Scope, Throwable, Byte] =
    featureCollection.featuresStream
      .map(GeoJSONWriter.toGeoJSON)
      .intersperse("\n")
      .via(ZPipeline.utf8Encode)

object GeoJSONSequenceService:

  val layer: ULayer[GeoJSONSequenceService] =
    ZLayer.derive[GeoJSONSequenceService]
