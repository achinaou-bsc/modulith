package dev.a4i.bsc.modulith.application.geo

import org.geotools.data.geojson.GeoJSONWriter
import org.geotools.data.simple.SimpleFeatureCollection
import os.*
import zio.*

class GeoJSONService:

  def write(featureCollection: SimpleFeatureCollection)(path: Path): UIO[Path] =
    ZIO.scoped:
      for
        outputStream <- ZIO.fromAutoCloseable(ZIO.attemptBlocking(os.write.over.outputStream(path))).orDie
        writer       <- ZIO.fromAutoCloseable(ZIO.attemptBlocking(GeoJSONWriter(outputStream))).orDie
        _            <- ZIO.attemptBlocking(writer.writeFeatureCollection(featureCollection)).orDie
      yield path

object GeoJSONService:

  val layer: ULayer[GeoJSONService] =
    ZLayer.derive[GeoJSONService]
