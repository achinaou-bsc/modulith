package dev.a4i.bsc.modulith.application

import org.geotools.api.data.Query
import org.geotools.api.data.SimpleFeatureSource
import org.geotools.api.filter.Filter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.ecql.ECQL
import zio.*

class WorldClimHistoricalTemperatureAverageRepository(dataSource: PostGISDataStore):

  private val featureSource: SimpleFeatureSource = dataSource.getFeatureSource(WorldClimHistoricalTemperatureAverageRepository.tableName)

  def findAll(limit: Option[Int] = None): UIO[SimpleFeatureCollection] =
    val filter: Filter = ECQL.toFilter:
      """
      true = true
      """

    val query: Query = Query(WorldClimHistoricalTemperatureAverageRepository.tableName, filter)

    limit match
      case Some(limit) => query.setMaxFeatures(limit)
      case None        => ()

    ZIO
      .attemptBlocking(featureSource.getFeatures(query))
      .orDie

object WorldClimHistoricalTemperatureAverageRepository:

  private val tableName: String = "worldclim_historical_temperature_average"

  val layer: URLayer[PostGISDataStore, WorldClimHistoricalTemperatureAverageRepository] =
    ZLayer.derive[WorldClimHistoricalTemperatureAverageRepository]
