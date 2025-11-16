package com.fakedatagen.jdbc

import com.fakedatagen.domain.{ActivityLog, Proposal, DatabaseConfig}
import com.typesafe.scalalogging.LazyLogging

import java.sql.{Connection, DriverManager, PreparedStatement, Timestamp}
import scala.util.{Try, Using}

/**
 * JDBC-based repository demonstrating low-level database operations.
 *
 * This class shows:
 * - Manual connection management
 * - PreparedStatement usage for SQL injection prevention
 * - Batch operations for efficiency
 * - Resource management with Using (try-with-resources)
 * - Type-safe parameter binding
 *
 * Compare this with the Slick implementation to understand the
 * tradeoffs between low-level control and high-level abstraction.
 */
class JdbcRepository(dbConfig: DatabaseConfig) extends LazyLogging:

  /**
   * Establishes a JDBC connection to the database.
   *
   * Learning points:
   * - Class.forName loads the JDBC driver
   * - DriverManager.getConnection creates the connection
   * - Connection is a mutable resource that must be closed
   */
  private def getConnection: Connection =
    Class.forName(dbConfig.driver)
    DriverManager.getConnection(
      dbConfig.url,
      dbConfig.user,
      dbConfig.password
    )

  /**
   * Inserts a batch of ActivityLog records using JDBC batch operations.
   *
   * Learning points:
   * - PreparedStatement prevents SQL injection by separating SQL from data
   * - Batch operations group multiple inserts into one round-trip
   * - setInt, setString, setTimestamp demonstrate type-safe parameter binding
   * - executeBatch returns an array of update counts
   * - Using ensures resources are properly closed even on exception
   *
   * @param activities List of ActivityLog records to insert
   * @return Either containing error message or count of inserted records
   */
  def insertActivityLogs(activities: List[ActivityLog]): Either[String, Int] =
    val sql = """
      INSERT INTO activity_logs (
        id, status, user_id, industry_name, tenant_location,
        success_probability, account_id, created_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    """

    Try {
      Using.resource(getConnection) { connection =>
        // Disable auto-commit for better batch performance
        connection.setAutoCommit(false)

        Using.resource(connection.prepareStatement(sql)) { stmt =>
          activities.foreach { activity =>
            // Bind parameters by position (1-indexed)
            stmt.setInt(1, activity.id)
            stmt.setString(2, activity.status)
            stmt.setInt(3, activity.userId)
            stmt.setString(4, activity.industryName)
            stmt.setString(5, activity.tenantLocation)
            stmt.setInt(6, activity.successProbability)
            stmt.setInt(7, activity.accountId)
            stmt.setTimestamp(8, Timestamp.valueOf(activity.createdAt))

            // Add to batch instead of executing immediately
            stmt.addBatch()
          }

          // Execute all batched statements
          val results = stmt.executeBatch()
          connection.commit()

          logger.info(s"Inserted ${results.sum} activity logs via JDBC")
          results.sum
        }
      }
    }.toEither.left.map { ex =>
      logger.error(s"Failed to insert activity logs via JDBC: ${ex.getMessage}", ex)
      s"JDBC insert failed: ${ex.getMessage}"
    }

  /**
   * Inserts a batch of Proposal records using JDBC batch operations.
   *
   * @param proposals List of Proposal records to insert
   * @return Either containing error message or count of inserted records
   */
  def insertProposals(proposals: List[Proposal]): Either[String, Int] =
    val sql = """
      INSERT INTO proposals (
        id, activity_log_id, last_entered, edit_locked,
        proposal_type, created_at
      ) VALUES (?, ?, ?, ?, ?, ?)
    """

    Try {
      Using.resource(getConnection) { connection =>
        connection.setAutoCommit(false)

        Using.resource(connection.prepareStatement(sql)) { stmt =>
          proposals.foreach { proposal =>
            stmt.setInt(1, proposal.id)
            stmt.setInt(2, proposal.activityLogId)
            stmt.setBoolean(3, proposal.lastEntered)
            stmt.setBoolean(4, proposal.editLocked)
            stmt.setString(5, proposal.proposalType)
            stmt.setTimestamp(6, Timestamp.valueOf(proposal.createdAt))

            stmt.addBatch()
          }

          val results = stmt.executeBatch()
          connection.commit()

          logger.info(s"Inserted ${results.sum} proposals via JDBC")
          results.sum
        }
      }
    }.toEither.left.map { ex =>
      logger.error(s"Failed to insert proposals via JDBC: ${ex.getMessage}", ex)
      s"JDBC insert failed: ${ex.getMessage}"
    }

  /**
   * Counts total records in a table.
   *
   * Learning points:
   * - executeQuery for SELECT statements
   * - ResultSet navigation with next()
   * - Type-safe column access with getInt
   *
   * @param tableName Name of the table to count
   * @return Either containing error or record count
   */
  def countRecords(tableName: String): Either[String, Int] =
    Try {
      Using.resource(getConnection) { connection =>
        Using.resource(connection.createStatement()) { stmt =>
          Using.resource(stmt.executeQuery(s"SELECT COUNT(*) FROM $tableName")) { rs =>
            if rs.next() then rs.getInt(1) else 0
          }
        }
      }
    }.toEither.left.map(ex => s"Count query failed: ${ex.getMessage}")

  /**
   * Gets the maximum ID from a table, or 0 if table is empty.
   *
   * @param tableName Name of the table to query
   * @return Either containing error message or max ID (0 if empty)
   */
  def getMaxId(tableName: String): Either[String, Int] =
    Try {
      Using.resource(getConnection) { conn =>
        Using.resource(conn.createStatement()) { stmt =>
          val rs = stmt.executeQuery(s"SELECT COALESCE(MAX(id), 0) FROM $tableName")
          if rs.next() then rs.getInt(1)
          else 0
        }
      }
    }.toEither.left.map(ex => s"Failed to get max ID from $tableName: ${ex.getMessage}")

  /**
   * Verifies database connectivity and schema existence.
   *
   * @return Either containing error message or success message
   */
  def testConnection(): Either[String, String] =
    Try {
      Using.resource(getConnection) { connection =>
        val metadata = connection.getMetaData
        s"Connected to ${metadata.getDatabaseProductName} ${metadata.getDatabaseProductVersion}"
      }
    }.toEither.left.map(ex => s"Connection test failed: ${ex.getMessage}")
