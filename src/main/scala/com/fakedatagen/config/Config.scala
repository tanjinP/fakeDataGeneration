package com.fakedatagen.config

import com.fakedatagen.domain.DatabaseConfig
import com.typesafe.config.ConfigFactory
import scala.util.{Try, Success, Failure}

/**
 * Configuration loader for database and application settings.
 *
 * This object demonstrates Scala's ability to load configuration
 * from external files using Typesafe Config library.
 */
object Config:

  /**
   * Loads database configuration from application.conf
   *
   * @return Either containing error message or DatabaseConfig
   */
  def loadDatabaseConfig(): Either[String, DatabaseConfig] =
    Try {
      val config = ConfigFactory.load()
      DatabaseConfig(
        url = config.getString("database.url"),
        driver = config.getString("database.driver"),
        user = config.getString("database.user"),
        password = config.getString("database.password")
      )
    } match
      case Success(dbConfig) => Right(dbConfig)
      case Failure(exception) =>
        Left(s"Failed to load database configuration: ${exception.getMessage}")

  /**
   * Gets a configuration value with a default fallback.
   *
   * @param path The configuration path
   * @param default Default value if path doesn't exist
   * @return The configuration value or default
   */
  def getInt(path: String, default: Int): Int =
    Try(ConfigFactory.load().getInt(path)).getOrElse(default)

  def getString(path: String, default: String): String =
    Try(ConfigFactory.load().getString(path)).getOrElse(default)

  def getBoolean(path: String, default: Boolean): Boolean =
    Try(ConfigFactory.load().getBoolean(path)).getOrElse(default)
