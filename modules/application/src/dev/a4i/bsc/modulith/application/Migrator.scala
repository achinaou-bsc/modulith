package dev.a4i.bsc.modulith.application

import javax.sql.DataSource

import org.flywaydb.core.Flyway
import zio.*

class Migrator(dataSource: DataSource):

  val migrate: UIO[Unit] =
    ZIO
      .attemptBlocking:
        Flyway.configure
          .dataSource(dataSource)
          .load
          .migrate
      .unit
      .orDie

object Migrator:

  val layer: URLayer[DataSource, Migrator] =
    ZLayer.derive[Migrator]
