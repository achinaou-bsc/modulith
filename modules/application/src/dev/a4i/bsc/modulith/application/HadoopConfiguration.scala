package dev.a4i.bsc.modulith.application

import org.apache.hadoop.conf.Configuration as HadoopNativeConfiguration
import zio.*
import zio.config.magnolia.deriveConfig

type HadoopConfiguration = HadoopNativeConfiguration

object HadoopConfiguration:

  val layer: ULayer[HadoopConfiguration] =
    ZLayer:
      for
        configuration      <- ZIO.config[Configuration].orDie
        _                  <- ZIO.logInfo(s"Using Hadoop configuration: ${configuration.hadoopProperties}")
        hadoopConfiguration = new HadoopNativeConfiguration:
                                configuration.hadoopProperties.foreach(set(_, _))
      yield hadoopConfiguration

  case class Configuration(
      fsDefaultFS: String
  ):

    def hadoopProperties: Map[String, String] =
      Map("fs.defaultFS" -> fsDefaultFS)

  object Configuration:

    given Config[Configuration] = deriveConfig.nested("hadoop", "configuration")
