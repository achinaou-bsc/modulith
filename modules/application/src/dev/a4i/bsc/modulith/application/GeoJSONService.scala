package dev.a4i.bsc.modulith.application

import java.io.IOException

import org.geotools.data.geojson.GeoJSONWriter
import org.geotools.data.simple.SimpleFeatureCollection
import os.*
import zio.*

class GeoJSONService:

  def write(featureCollection: SimpleFeatureCollection)(path: Path): IO[IOException, Path] =
    ZIO.scoped:
      for
        outputStream <- ZIO.fromAutoCloseable:
                          ZIO.attemptBlockingIO:
                            os.write.over.outputStream(path)
        writer       <- ZIO.fromAutoCloseable:
                          ZIO.attemptBlockingIO:
                            GeoJSONWriter(outputStream)
        _            <- ZIO.attemptBlockingIO:
                          writer.writeFeatureCollection(featureCollection)
      yield path

object GeoJSONService:

  val layer: ULayer[GeoJSONService] =
    ZLayer.derive[GeoJSONService]
