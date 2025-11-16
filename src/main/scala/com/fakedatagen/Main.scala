package com.fakedatagen

import com.fakedatagen.config.Config
import com.fakedatagen.domain.GenerationConfig
import com.fakedatagen.generators.DataGenerators
import com.fakedatagen.jdbc.JdbcRepository
import com.fakedatagen.slick.SlickRepository
import com.typesafe.scalalogging.LazyLogging
import com.monovore.decline.*
import cats.implicits.*

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Main application demonstrating both JDBC and Slick approaches.
 *
 * This application shows:
 * - CLI argument parsing with Decline
 * - Configuration loading from application.conf
 * - Type-safe data generation
 * - Comparison between JDBC and Slick
 * - Progress reporting
 * - Error handling with Either
 */
object Main
    extends CommandApp(
      name = "fakedata-generator",
      header = "Generate fake data for database testing",
      main = {
        // CLI argument definitions
        val recordCountOpt = Opts
          .option[Int](
            "records",
            short = "r",
            help = "Number of records to generate (default: 10000)"
          )
          .withDefault(10000)

        val batchSizeOpt = Opts
          .option[Int](
            "batch-size",
            short = "b",
            help = "Batch size for inserts (default: 5000)"
          )
          .withDefault(5000)

        val methodOpt = Opts
          .option[String](
            "method",
            short = "m",
            help = "Database access method: jdbc, slick, or both (default: both)"
          )
          .withDefault("both")

        val testConnectionOpt = Opts
          .flag(
            "test-connection",
            short = "t",
            help = "Test database connection and exit"
          )
          .orFalse

        // Combine options into a configuration
        (recordCountOpt, batchSizeOpt, methodOpt, testConnectionOpt).mapN {
          (records, batchSize, method, testConnection) =>
            Main.run(records, batchSize, method, testConnection)
        }
      }
    )
    with LazyLogging:

  def run(
    recordCount: Int,
    batchSize: Int,
    method: String,
    testConnection: Boolean
  ): Unit =
    logger.info("=" * 60)
    logger.info("Fake Data Generation Tool")
    logger.info("=" * 60)

    // Load database configuration
    val dbConfigResult = Config.loadDatabaseConfig()
    dbConfigResult match
      case Left(error) =>
        logger.error(s"Configuration error: $error")
        System.exit(1)

      case Right(dbConfig) =>
        logger.info(s"Loaded database configuration for: ${dbConfig.url}")

        // Test connection if requested
        if testConnection then
          testConnections(dbConfig)
          System.exit(0)

        // Determine which methods to use
        val useJdbc = method == "jdbc" || method == "both"
        val useSlick = method == "slick" || method == "both"

        logger.info(s"Generating $recordCount records in batches of $batchSize")
        logger.info(s"Using: ${if useJdbc then "JDBC " else ""}${if useSlick then "Slick" else ""}")
        logger.info("-" * 60)

        // Run JDBC implementation
        if useJdbc then runJdbcImplementation(dbConfig, recordCount, batchSize)

        // Run Slick implementation
        if useSlick then runSlickImplementation(dbConfig, recordCount, batchSize)

        logger.info("=" * 60)
        logger.info("Generation complete!")

  /** Tests database connectivity for both JDBC and Slick. */
  private def testConnections(dbConfig: com.fakedatagen.domain.DatabaseConfig): Unit =
    logger.info("Testing database connections...")

    // Test JDBC
    val jdbcRepo = JdbcRepository(dbConfig)
    jdbcRepo.testConnection() match
      case Right(msg) => logger.info(s"✓ JDBC: $msg")
      case Left(error) => logger.error(s"✗ JDBC: $error")

    // Test Slick
    val slickRepo = SlickRepository(dbConfig)
    slickRepo.testConnection() match
      case Right(msg) => logger.info(s"✓ Slick: $msg")
      case Left(error) => logger.error(s"✗ Slick: $error")

    slickRepo.close()

  /** Runs the JDBC implementation with progress tracking. */
  private def runJdbcImplementation(
    dbConfig: com.fakedatagen.domain.DatabaseConfig,
    recordCount: Int,
    batchSize: Int
  ): Unit =
    logger.info("Starting JDBC implementation...")
    val startTime = System.currentTimeMillis()

    val repo = JdbcRepository(dbConfig)

    // Get current max IDs to avoid conflicts
    val maxActivityLogId = repo.getMaxId("activity_logs").getOrElse(0)
    val maxProposalId = repo.getMaxId("proposals").getOrElse(0)

    logger.info(
      s"Current max IDs - ActivityLogs: $maxActivityLogId, Proposals: $maxProposalId"
    )

    // Generate and insert ActivityLogs starting from max_id + 1
    logger.info("Generating ActivityLogs...")
    val activityBatches = DataGenerators.generateInBatches(
      recordCount,
      batchSize,
      (startId, count) => DataGenerators.generateActivityLogs(maxActivityLogId + startId, count)
    )

    var totalInserted = 0
    var batchNum = 1
    activityBatches.foreach { batch =>
      logger.info(s"  Inserting batch $batchNum (${batch.size} records)...")
      repo.insertActivityLogs(batch) match
        case Right(count) => totalInserted += count
        case Left(error) => logger.error(s"  Failed: $error")
      batchNum += 1
    }

    logger.info(s"Inserted $totalInserted ActivityLogs via JDBC")

    // Generate and insert Proposals starting from max_id + 1
    logger.info("Generating Proposals...")
    val proposalBatches = DataGenerators.generateInBatches(
      recordCount,
      batchSize,
      (startId, count) =>
        DataGenerators.generateProposals(
          maxProposalId + startId,
          count,
          maxActivityLogId + 1,
          maxActivityLogId + recordCount
        )
    )

    totalInserted = 0
    batchNum = 1
    proposalBatches.foreach { batch =>
      logger.info(s"  Inserting batch $batchNum (${batch.size} records)...")
      repo.insertProposals(batch) match
        case Right(count) => totalInserted += count
        case Left(error) => logger.error(s"  Failed: $error")
      batchNum += 1
    }

    logger.info(s"Inserted $totalInserted Proposals via JDBC")

    val duration = (System.currentTimeMillis() - startTime) / 1000.0
    logger.info(f"JDBC implementation completed in $duration%.2f seconds")
    logger.info("-" * 60)

  /** Runs the Slick implementation with progress tracking. */
  private def runSlickImplementation(
    dbConfig: com.fakedatagen.domain.DatabaseConfig,
    recordCount: Int,
    batchSize: Int
  ): Unit =
    logger.info("Starting Slick implementation...")
    val startTime = System.currentTimeMillis()

    val repo = SlickRepository(dbConfig)

    try
      // Get current max IDs to avoid conflicts
      val maxActivityLogId = repo.getMaxId("activity_logs").getOrElse(0)
      val maxProposalId = repo.getMaxId("proposals").getOrElse(0)

      logger.info(
        s"Current max IDs - ActivityLogs: $maxActivityLogId, Proposals: $maxProposalId"
      )

      // Generate and insert ActivityLogs starting from max_id + 1
      logger.info("Generating ActivityLogs...")
      val activityBatches = DataGenerators.generateInBatches(
        recordCount,
        batchSize,
        (startId, count) => DataGenerators.generateActivityLogs(maxActivityLogId + startId, count)
      )

      var totalInserted = 0
      var batchNum = 1
      activityBatches.foreach { batch =>
        logger.info(s"  Inserting batch $batchNum (${batch.size} records)...")
        repo.insertActivityLogs(batch) match
          case Right(count) => totalInserted += count
          case Left(error) => logger.error(s"  Failed: $error")
        batchNum += 1
      }

      logger.info(s"Inserted $totalInserted ActivityLogs via Slick")

      // Generate and insert Proposals starting from max_id + 1
      logger.info("Generating Proposals...")
      val proposalBatches = DataGenerators.generateInBatches(
        recordCount,
        batchSize,
        (startId, count) =>
          DataGenerators.generateProposals(
            maxProposalId + startId,
            count,
            maxActivityLogId + 1,
            maxActivityLogId + recordCount
          )
      )

      totalInserted = 0
      batchNum = 1
      proposalBatches.foreach { batch =>
        logger.info(s"  Inserting batch $batchNum (${batch.size} records)...")
        repo.insertProposals(batch) match
          case Right(count) => totalInserted += count
          case Left(error) => logger.error(s"  Failed: $error")
        batchNum += 1
      }

      logger.info(s"Inserted $totalInserted Proposals via Slick")

      val duration = (System.currentTimeMillis() - startTime) / 1000.0
      logger.info(f"Slick implementation completed in $duration%.2f seconds")
      logger.info("-" * 60)

    finally repo.close()
