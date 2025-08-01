package dev.a4i.bsc.modulith.application

import org.geotools.api.data.Query
import org.geotools.api.data.SimpleFeatureSource
import org.geotools.api.filter.Filter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.ecql.ECQL
import zio.*

private class WADAridityRepository(dataSource: PostGISDataStore):

  private val featureSource: SimpleFeatureSource = dataSource.getFeatureSource(WADAridityRepository.tableName)

  def findAll(): UIO[SimpleFeatureCollection] =
    val filter: Filter = ECQL.toFilter:
      """
      true = true
      """

    val query: Query = Query(WADAridityRepository.tableName, filter)

    ZIO
      .attemptBlockingIO(featureSource.getFeatures(query))
      .orDie

object WADAridityRepository:

  private val tableName: String = "wad_aridity"

  val layer: URLayer[PostGISDataStore, WADAridityRepository] =
    ZLayer.derive[WADAridityRepository]
