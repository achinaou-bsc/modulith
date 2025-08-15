package dev.a4i.bsc.modulith.application

import com.augustnagro.magnum.SqlLogger
import com.augustnagro.magnum.magzio.TransactorZIO
import zio.*
import zio.http.*
import zio.logging.backend.SLF4J

import dev.a4i.bsc.modulith.application.geo.*
import dev.a4i.bsc.modulith.application.github.*
import dev.a4i.bsc.modulith.application.hadoop.*
import dev.a4i.bsc.modulith.application.persistence.*
import dev.a4i.bsc.modulith.application.postgis.*
import dev.a4i.bsc.modulith.application.route.*
import dev.a4i.bsc.modulith.application.service.*

object Application extends ZIOAppDefault:

  override val bootstrap: ZLayer[Any, Any, Any] =
    Runtime.enableAutoBlockingExecutor ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[Any, Throwable, Unit] =
    program
      .provideSomeAuto(
        DataSource.layer,
        DynamicRouter.layer,
        GeoJSONSequenceService.layer,
        GitHub.layer,
        HadoopConfiguration.layer,
        HadoopFileSystem.layer,
        HttpClient.layer,
        JobArtifactManager.layer,
        JobRepository.layer,
        JobRouter.layer,
        JobService.layer,
        JobStatusSynchronizer.layer,
        JobSubmitter.layer,
        Migrator.layer,
        PolygonOverlayHadoopMapReduceGridService.layer,
        PolygonOverlayHadoopMapReduceNaiveService.layer,
        PolygonOverlayHadoopMapReduceService.layer,
        PostGISDataStore.layer,
        Router.layer,
        Server.default,
        StaticRouter.layer,
        TransactorZIO.layer(SqlLogger.NoOp),
        WADAridityRepository.layer,
        WorldClimHistoricalTemperatureAverageRepository.layer,
        YarnClient.layer
      )
      .unit

  private lazy val program =
    for
      _ <- ZIO.serviceWithZIO[Migrator](_.migrate)
      _ <- ZIO.serviceWithZIO[Router](router => Server.serve(router.routes @@ ErrorResponseConfig.debug))
    yield ()
