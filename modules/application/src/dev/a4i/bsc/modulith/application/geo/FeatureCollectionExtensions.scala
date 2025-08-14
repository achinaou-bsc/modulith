package dev.a4i.bsc.modulith.application.geo

import java.io.Closeable

import org.geotools.api.feature.`type`.FeatureType
import org.geotools.api.feature.Feature
import org.geotools.feature.FeatureCollection
import org.geotools.feature.FeatureIterator
import zio.*
import zio.stream.ZStream

object FeatureCollectionExtensions:

  extension [T <: FeatureType, F <: Feature](featureCollection: FeatureCollection[T, F])

    def featuresIterator: Iterator[F] & Closeable =
      val featureIterator: FeatureIterator[F] = featureCollection.features

      new Iterator[F] with Closeable:

        override def hasNext: Boolean =
          featureIterator.hasNext

        override def next: F =
          featureIterator.next

        override def close: Unit =
          featureIterator.close

    def featuresStream: ZStream[Scope, Throwable, F] =
      ZStream.fromIteratorZIO(
        ZIO.fromAutoCloseable(ZIO.attempt(featureCollection.featuresIterator)),
        chunkSize = 1
      )
