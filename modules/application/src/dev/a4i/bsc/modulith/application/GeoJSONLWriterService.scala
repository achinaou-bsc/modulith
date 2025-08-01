package dev.a4i.bsc.modulith.application

import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.geojson.feature.FeatureJSON
import os.*
import zio.*
import zio.stream.*

class GeoJSONLWriterService:

  def write(geoJSONLFile: Path, featureCollection: SimpleFeatureCollection): Task[Path] =
    ZIO.scoped:
      for
        featureJSON       = FeatureJSON()
        featuresIterator <- ZIO.fromAutoCloseable:
                              ZIO.attemptBlockingIO:
                                featureCollection.features
        _                <- ZStream
                              .unfoldZIO(featuresIterator): iterator =>
                                ZIO.whenZIO(ZIO.attemptBlockingIO(iterator.hasNext))(ZIO.attemptBlockingIO((iterator.next, iterator)))
                              .map(featureJSON.toString)
                              .intersperse("\n")
                              .via(ZPipeline.utf8Encode)
                              .run(ZSink.fromFile(geoJSONLFile.toIO))
      yield geoJSONLFile

object GeoJSONLWriterService:

  val layer: ULayer[GeoJSONLWriterService] =
    ZLayer.derive[GeoJSONLWriterService]
