package dev.a4i.bsc.modulith.application.hadoop

import org.apache.hadoop.conf.Configuration as HadoopNativeConfiguration
import org.apache.hadoop.fs.Path
import zio.*
import zio.config.*
import zio.config.magnolia.DeriveConfig
import zio.config.magnolia.deriveConfig

import dev.a4i.bsc.modulith.application.hadoop.HadoopConfiguration.Configuration.PortRange

type HadoopConfiguration = HadoopNativeConfiguration

object HadoopConfiguration:

  val layer: ULayer[HadoopConfiguration] =
    ZLayer:
      for
        configuration      <- ZIO.config[Configuration].orDie
        hadoopConfiguration = new HadoopNativeConfiguration(false):
                                addResource(configuration.coreSite)
                                addResource(configuration.hdfsSite)
                                addResource(configuration.yarnSite)
                                set("mapreduce.framework.name", "yarn")
                                set("mapreduce.jvm.add-opens-as-default", "false")
                                set("dfs.client.use.datanode.hostname", "true")
                                set("fs.AbstractFileSystem.hdfs.impl", "org.apache.hadoop.fs.Hdfs")

                                configuration.mapredDefault.yarnAppMapreduceAmJobClientPortRange match
                                  case Some(PortRange(start, end)) =>
                                    set("yarn.app.mapreduce.am.job.client.port-range", s"$start-$end")
                                  case None                        =>
                                    ()
      yield hadoopConfiguration

  case class Configuration(
      coreSite: Path,
      hdfsSite: Path,
      yarnSite: Path,
      mapredDefault: Configuration.MapredDefault
  )

  object Configuration:

    given Config[Configuration] =
      given DeriveConfig[Path] = DeriveConfig[String].map(Path(_))

      deriveConfig[Configuration]
        .nested("hadoop", "configuration")
        .mapKey(toKebabCase)

    case class MapredDefault(
        yarnAppMapreduceAmJobClientPortRange: Option[PortRange]
    )

    case class PortRange(start: Int, end: Int)
