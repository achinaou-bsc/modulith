package dev.a4i.bsc.modulith.application.postgis

import org.geotools.api.data.Query
import org.geotools.api.data.SimpleFeatureSource
import org.geotools.api.filter.Filter
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.filter.text.ecql.ECQL
import zio.*

import dev.a4i.bsc.modulith.application.postgis.PostGISDataStore
import dev.a4i.bsc.modulith.application.postgis.WorldClimHistoricalTemperatureAverageRepository.TemperatureValuePredicate

class WorldClimHistoricalTemperatureAverageRepository(dataSource: PostGISDataStore):

  private val featureSource: SimpleFeatureSource =
    dataSource.getFeatureSource(WorldClimHistoricalTemperatureAverageRepository.tableName)

  def findAll(limit: Option[Int]): UIO[SimpleFeatureCollection] =
    val query: Query = Query(WorldClimHistoricalTemperatureAverageRepository.tableName)

    limit match
      case Some(limit) => query.setMaxFeatures(limit)
      case None        => ()

    ZIO
      .attemptBlocking(featureSource.getFeatures(query))
      .orDie

  def findAll(temperatureValuePredicate: TemperatureValuePredicate, limit: Option[Int]): UIO[SimpleFeatureCollection] =
    val filter: Filter = ECQL.toFilter:
      temperatureValuePredicate match
        case TemperatureValuePredicate.LessThan(value)             => s"value < $value"
        case TemperatureValuePredicate.LessThanOrEqualTo(value)    => s"value <= $value"
        case TemperatureValuePredicate.EqualTo(value)              => s"value = $value"
        case TemperatureValuePredicate.GreaterThanOrEqualTo(value) => s"value >= $value"
        case TemperatureValuePredicate.GreaterThan(value)          => s"value > $value"
    val query: Query   = Query(WorldClimHistoricalTemperatureAverageRepository.tableName, filter)

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

  enum TemperatureValuePredicate:
    case LessThan(value: Double)
    case LessThanOrEqualTo(value: Double)
    case EqualTo(value: Double)
    case GreaterThanOrEqualTo(value: Double)
    case GreaterThan(value: Double)
