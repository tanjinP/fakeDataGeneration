package com.fakedatagen.slick

import com.fakedatagen.domain.{ActivityLog, Proposal, DatabaseConfig}
import com.fakedatagen.slick.Tables.*
import com.typesafe.scalalogging.LazyLogging
import slick.jdbc.PostgresProfile.api.*

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*
import scala.util.{Try, Success, Failure}

/**
 * Slick-based repository demonstrating functional relational mapping.
 *
 * This class shows:
 * - Type-safe query construction
 * - Asynchronous database operations with Future
 * - Automatic SQL generation from Scala code
 * - Connection pooling with HikariCP
 * - Composable queries
 *
 * Compare this with JDBC to see how Slick eliminates SQL strings
 * and provides compile-time query validation.
 */
class SlickRepository(dbConfig: DatabaseConfig)(using ec: ExecutionContext) extends LazyLogging:

  /**
   * Database instance with connection pooling.
   *
   * Learning points:
   * - Database.forURL creates a connection pool
   * - HikariCP manages connections efficiently
   * - Slick uses this for all database operations
   */
  private val db = Database.forURL(
    url = dbConfig.url,
    driver = dbConfig.driver,
    user = dbConfig.user,
    password = dbConfig.password,
    executor = AsyncExecutor(
      name = "AsyncExecutor",
      numThreads = 10,
      queueSize = 1000
    )
  )

  /**
   * Inserts a batch of ActivityLog records using Slick.
   *
   * Learning points:
   * - ++= is Slick's batch insert operator
   * - DBIO represents a database action (not yet executed)
   * - db.run executes the action and returns a Future
   * - Slick generates the INSERT SQL automatically
   * - Type safety: compiler ensures we're inserting ActivityLog, not anything else
   *
   * @param activities List of ActivityLog records to insert
   * @return Either containing error message or count of inserted records
   */
  def insertActivityLogs(activities: List[ActivityLog]): Either[String, Int] =
    // Define the database action (doesn't execute yet)
    val action = activityLogs ++= activities

    // Execute the action and wait for completion
    Try {
      val future: Future[Option[Int]] = db.run(action)
      Await.result(future, 60.seconds)
    } match
      case Success(Some(count)) =>
        logger.info(s"Inserted $count activity logs via Slick")
        Right(count)
      case Success(None) =>
        logger.info(s"Inserted ${activities.size} activity logs via Slick")
        Right(activities.size)
      case Failure(ex) =>
        logger.error(s"Failed to insert activity logs via Slick: ${ex.getMessage}", ex)
        Left(s"Slick insert failed: ${ex.getMessage}")

  /**
   * Inserts a batch of Proposal records using Slick.
   *
   * @param proposalList List of Proposal records to insert
   * @return Either containing error message or count of inserted records
   */
  def insertProposals(proposalList: List[Proposal]): Either[String, Int] =
    val action = proposals ++= proposalList

    Try {
      val future: Future[Option[Int]] = db.run(action)
      Await.result(future, 60.seconds)
    } match
      case Success(Some(count)) =>
        logger.info(s"Inserted $count proposals via Slick")
        Right(count)
      case Success(None) =>
        logger.info(s"Inserted ${proposalList.size} proposals via Slick")
        Right(proposalList.size)
      case Failure(ex) =>
        logger.error(s"Failed to insert proposals via Slick: ${ex.getMessage}", ex)
        Left(s"Slick insert failed: ${ex.getMessage}")

  /**
   * Counts total records in activity_logs table.
   *
   * Learning points:
   * - length is a Slick aggregate function
   * - result returns the computed value
   * - Slick generates: SELECT COUNT(*) FROM activity_logs
   *
   * @return Either containing error or record count
   */
  def countActivityLogs(): Either[String, Int] =
    val query = activityLogs.length.result

    Try {
      Await.result(db.run(query), 10.seconds)
    }.toEither.left.map(ex => s"Count query failed: ${ex.getMessage}")

  /**
   * Counts total records in proposals table.
   *
   * @return Either containing error or record count
   */
  def countProposals(): Either[String, Int] =
    val query = proposals.length.result

    Try {
      Await.result(db.run(query), 10.seconds)
    }.toEither.left.map(ex => s"Count query failed: ${ex.getMessage}")

  /**
   * Example of a type-safe join query.
   *
   * Learning points:
   * - Slick supports SQL joins with type safety
   * - The relationship is defined by matching columns
   * - Results are typed tuples
   *
   * This demonstrates Slick's power over JDBC: joins are
   * checked at compile-time for type correctness.
   *
   * @return List of (ActivityLog, Proposal) pairs
   */
  def getActivityLogsWithProposals(): Either[String, List[(ActivityLog, Proposal)]] =
    val query = for {
      activity <- activityLogs
      proposal <- proposals if proposal.activityLogId === activity.id
    } yield (activity, proposal)

    Try {
      Await.result(db.run(query.result), 30.seconds).toList
    }.toEither.left.map(ex => s"Join query failed: ${ex.getMessage}")

  /**
   * Gets the maximum ID from a table, or 0 if table is empty.
   *
   * @param tableName Name of the table to query
   * @return Either containing error message or max ID (0 if empty)
   */
  def getMaxId(tableName: String): Either[String, Int] =
    val query = tableName match
      case "activity_logs" => sql"SELECT COALESCE(MAX(id), 0) FROM activity_logs".as[Int].head
      case "proposals" => sql"SELECT COALESCE(MAX(id), 0) FROM proposals".as[Int].head
      case _ => sql"SELECT 0".as[Int].head

    Try {
      Await.result(db.run(query), 10.seconds)
    }.toEither.left.map(ex => s"Failed to get max ID from $tableName: ${ex.getMessage}")

  /**
   * Closes the database connection pool.
   *
   * Important: Call this when shutting down the application
   * to properly release resources.
   */
  def close(): Unit =
    db.close()
    logger.info("Closed Slick database connection pool")

  /**
   * Verifies database connectivity.
   *
   * @return Either containing error message or success message
   */
  def testConnection(): Either[String, String] =
    val query = sql"SELECT version()".as[String].head

    Try {
      val version = Await.result(db.run(query), 10.seconds)
      s"Connected to PostgreSQL: $version"
    }.toEither.left.map(ex => s"Connection test failed: ${ex.getMessage}")
