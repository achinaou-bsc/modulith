package dev.a4i.bsc.modulith.application.geo

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.Using

import org.geotools.api.feature.`type`.GeometryDescriptor
import org.geotools.api.feature.FeatureVisitor
import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.api.filter.Filter
import org.geotools.api.filter.sort.SortBy
import org.geotools.api.util.ProgressListener
import org.geotools.data.DataUtilities
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.data.simple.SimpleFeatureIterator
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

final class FlattenedFeatureCollection(featureCollection: SimpleFeatureCollection)
    extends DecoratingSimpleFeatureCollection(featureCollection):

  override val size: Int = -1

  override def isEmpty: Boolean =
    Using(features)(!_.hasNext).get

  override lazy val getSchema: SimpleFeatureType =
    val delegateSchema: SimpleFeatureType                       = delegate.getSchema
    val delegateGeometryAttributeDescriptor: GeometryDescriptor = delegateSchema.getGeometryDescriptor

    val featureTypeBuilder: SimpleFeatureTypeBuilder = SimpleFeatureTypeBuilder()
    featureTypeBuilder.setName(delegateSchema.getName)

    delegateSchema.getAttributeDescriptors.forEach: delegateAttributeDescriptor =>
      if delegateAttributeDescriptor eq delegateGeometryAttributeDescriptor
      then
        featureTypeBuilder.setCRS(delegateSchema.getCoordinateReferenceSystem)
        featureTypeBuilder.add(
          delegateAttributeDescriptor.getLocalName,
          getIndividualGeometryClass(delegateAttributeDescriptor.getType.getBinding.asInstanceOf[Class[Geometry]])
        )
        featureTypeBuilder.setDefaultGeometry(delegateAttributeDescriptor.getLocalName)
      else featureTypeBuilder.add(delegateAttributeDescriptor)

    featureTypeBuilder.buildFeatureType

  override def features: SimpleFeatureIterator =
    new SimpleFeatureIterator:
      private val delegateFeatureIterator: SimpleFeatureIterator = featureCollection.features
      private val queue: mutable.Queue[SimpleFeature]            = mutable.Queue.empty
      private val featureBuilder: SimpleFeatureBuilder           = SimpleFeatureBuilder(getSchema)

      @tailrec
      private def queueNext: Unit =
        if queue.isEmpty && delegateFeatureIterator.hasNext
        then
          val delegateFeature: SimpleFeature = delegateFeatureIterator.next
          val delegateGeometry: Geometry     = delegateFeature.getDefaultGeometry.asInstanceOf[Geometry]

          if delegateGeometry == null || delegateGeometry.isEmpty
          then queueNext
          else
            getIndividualGeometries(delegateGeometry) match
              case Seq()                               => queueNext
              case Seq(_)                              => queue.enqueue(delegateFeature)
              case individualGeometries: Seq[Geometry] =>
                individualGeometries.iterator.zipWithIndex.foreach: (individualGeometry, index) =>
                  queue.enqueue:
                    featureBuilder.reset
                    featureBuilder.init(delegateFeature)
                    featureBuilder.set(getSchema.getGeometryDescriptor.getLocalName, individualGeometry)
                    featureBuilder.buildFeature(s"${delegateFeature.getID}_${index + 1}")

      override def hasNext: Boolean =
        queueNext
        queue.nonEmpty

      override def next: SimpleFeature =
        if hasNext
        then queue.dequeue
        else throw new NoSuchElementException("No more features in the collection")

      override def close: Unit =
        delegateFeatureIterator.close

  override protected def canDelegate(featureVisitor: FeatureVisitor): Boolean = false

  override def accepts(featureVisitor: FeatureVisitor, progressListener: ProgressListener) =
    DataUtilities.visit(this, featureVisitor, progressListener)

  override def subCollection(filter: Filter) =
    FlattenedFeatureCollection(delegate.subCollection(filter))

  override def sort(sortBy: SortBy) =
    FlattenedFeatureCollection(delegate.sort(sortBy))

  private def getIndividualGeometryClass(geometryClass: Class[? <: Geometry]): Class[? <: Geometry] =
    geometryClass match
      case c if c eq classOf[MultiPoint]         => classOf[Point]
      case c if c eq classOf[MultiLineString]    => classOf[LineString]
      case c if c eq classOf[MultiPolygon]       => classOf[Polygon]
      case c if c eq classOf[GeometryCollection] => classOf[Geometry]
      case _                                     => geometryClass

  private def getIndividualGeometries(geometry: Geometry): Seq[Geometry] =
    geometry match
      case multiGeometry: (MultiPoint | MultiLineString | MultiPolygon) =>
        (0 until multiGeometry.getNumGeometries).map(multiGeometry.getGeometryN)
      case geometryCollection: GeometryCollection                       =>
        (0 until geometryCollection.getNumGeometries).flatMap: i =>
          getIndividualGeometries(geometryCollection.getGeometryN(i))
      case _                                                            =>
        IndexedSeq(geometry)
