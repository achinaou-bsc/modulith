package dev.a4i.bsc.modulith.application

import org.apache.hadoop.yarn.client.api.YarnClient as HadoopYarnClient
import zio.*

type YarnClient = HadoopYarnClient

object YarnClient:

  val layer: URLayer[HadoopConfiguration, YarnClient] =
    ZLayer.scoped:
      for
        hadoopConfiguration <- ZIO.service[HadoopConfiguration]
        yarnClient          <- ZIO.fromAutoCloseable:
                                 ZIO.succeed:
                                   val yarnClient: YarnClient = HadoopYarnClient.createYarnClient()
                                   yarnClient.init(hadoopConfiguration)
                                   yarnClient
        _                   <- ZIO.attemptBlocking(yarnClient.start).orDie
      yield yarnClient
