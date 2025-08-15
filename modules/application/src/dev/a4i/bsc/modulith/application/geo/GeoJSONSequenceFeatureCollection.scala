package dev.a4i.bsc.modulith.application.geo

import java.io.BufferedReader
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Iterator
import scala.collection.mutable
import scala.util.Using

import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.data.geojson.GeoJSONReader
import org.geotools.data.simple.SimpleFeatureIterator
import org.geotools.feature.collection.AbstractFeatureCollection
import org.geotools.geometry.jts.ReferencedEnvelope
import zio.*

def supplierFromScoped(scoped: ZIO[Scope, Throwable, InputStream]): () => InputStream =
  () =>
    Unsafe.unsafe:
      case given Unsafe =>
        val scope: Scope.Closeable =
          Runtime.default.unsafe
            .run(Scope.make)
            .getOrThrowFiberFailure()

        val inputStream: InputStream =
          Runtime.default.unsafe
            .run(scope.extend(scoped))
            .getOrThrowFiberFailure()

        new FilterInputStream(inputStream):
          override def close: Unit =
            Runtime.default.unsafe
              .run(scope.close(Exit.unit))
              .getOrThrowFiberFailure()

class GeoJSONSequenceFeatureCollection(inputStream: () => InputStream) extends AbstractFeatureCollection(null):

  this.schema = getSchema

  override val size: Int = -1

  override lazy val getSchema: SimpleFeatureType =
    Using
      .apply(BufferedReader(InputStreamReader(inputStream(), StandardCharsets.UTF_8))): bufferedReader =>
        val line: String = bufferedReader.readLine

        if line != null
        then GeoJSONReader.parseFeature(line).getFeatureType
        else throw new IllegalStateException("GeoJSONL stream contains no features")
      .get

  override protected def openIterator: Iterator[SimpleFeature] & SimpleFeatureIterator =
    new Iterator[SimpleFeature] with SimpleFeatureIterator:
      private val bufferedReader: BufferedReader =
        BufferedReader(InputStreamReader(inputStream(), StandardCharsets.UTF_8))

      private val queue: mutable.Queue[SimpleFeature] =
        mutable.Queue.empty

      private def queueNext: Unit =
        if queue.isEmpty
        then
          val line: String = bufferedReader.readLine

          if line != null
          then queue.enqueue(GeoJSONReader.parseFeature(line))

      override def hasNext: Boolean =
        queueNext
        queue.nonEmpty

      override def next: SimpleFeature =
        if hasNext
        then queue.dequeue
        else throw new NoSuchElementException("No more features in the collection")

      override def close: Unit =
        bufferedReader.close

  override lazy val getBounds: ReferencedEnvelope =
    ReferencedEnvelope(getSchema.getCoordinateReferenceSystem)
