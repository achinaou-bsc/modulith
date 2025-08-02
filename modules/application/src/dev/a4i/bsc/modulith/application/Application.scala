package dev.a4i.bsc.modulith.application

import zio.*
import zio.logging.backend.SLF4J

object Application extends ZIOAppDefault:

  override val bootstrap: ZLayer[Any, Any, Any] =
    Runtime.enableAutoBlockingExecutor ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[Any, Throwable, Unit] =
    program.unit

  private lazy val program: ZIO[Any, Throwable, Unit] =
    for _ <- ZIO.log("Hello from the Modulith!")
    yield ()
