package dev.a4i.bsc.modulith.application.postgis

import org.geotools.api.data.Query
import org.geotools.api.data.SimpleFeatureSource
import org.geotools.api.filter.Filter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.ecql.ECQL
import zio.*

import dev.a4i.bsc.modulith.application.postgis.PostGISDataStore

class GlobalAiHistoricalAverageRepository(dataSource: PostGISDataStore):

  private val featureSource: SimpleFeatureSource =
    dataSource.getFeatureSource(GlobalAiHistoricalAverageRepository.tableName)

  def findAll(limit: Option[Int]): UIO[SimpleFeatureCollection] =
    val query: Query = Query(GlobalAiHistoricalAverageRepository.tableName)

    limit match
      case Some(limit) => query.setMaxFeatures(limit)
      case None        => ()

    ZIO
      .attemptBlocking(featureSource.getFeatures(query))
      .orDie

  def findAll(predicate: String, limit: Option[Int]): UIO[SimpleFeatureCollection] =
    val filter: Filter = ECQL.toFilter(predicate)
    val query: Query   = Query(GlobalAiHistoricalAverageRepository.tableName, filter)

    limit match
      case Some(limit) => query.setMaxFeatures(limit)
      case None        => ()

    ZIO
      .attemptBlocking(featureSource.getFeatures(query))
      .orDie

object GlobalAiHistoricalAverageRepository:

  private val tableName: String = "global_ai_historical_average"

  val layer: URLayer[PostGISDataStore, GlobalAiHistoricalAverageRepository] =
    ZLayer.derive[GlobalAiHistoricalAverageRepository]
