package dev.a4i.bsc.modulith.application

import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geojson.feature.FeatureJSON
import zio.*
import zio.stream.*

import dev.a4i.bsc.modulith.application.FeatureCollectionExtensions.*

class GeoJSONLService:

  def encode(featureCollection: SimpleFeatureCollection): ZStream[Scope, Throwable, Byte] =
    val featureJSON: FeatureJSON = FeatureJSON()

    featureCollection.featuresStream
      .map(featureJSON.toString)
      .intersperse("\n")
      .via(ZPipeline.utf8Encode)

object GeoJSONLService:

  val layer: ULayer[GeoJSONLService] =
    ZLayer.derive[GeoJSONLService]
