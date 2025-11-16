package com.fakedatagen.slick

import com.fakedatagen.domain.{ActivityLog, Proposal}
import slick.jdbc.PostgresProfile.api.*

import java.time.LocalDateTime

/**
 * Slick table definitions for type-safe database access.
 *
 * Learning points:
 * - Table[T] maps database tables to Scala case classes
 * - O.PrimaryKey defines primary key constraints
 * - Column types are checked at compile-time
 * - The * projection defines the default mapping
 * - <> operator maps between tuples and case classes
 *
 * Compare with JDBC: Here we define the schema once, and Slick
 * generates type-safe queries. With JDBC, we write SQL strings manually.
 */

/**
 * Table definition for activity_logs.
 *
 * Each column method creates a typed column definition that
 * Slick uses to generate SQL and validate queries at compile-time.
 */
class ActivityLogTable(tag: Tag) extends Table[ActivityLog](tag, "activity_logs"):

  // Column definitions with types matching the domain model
  def id = column[Int]("id", O.PrimaryKey)
  def status = column[String]("status")
  def userId = column[Int]("user_id")
  def industryName = column[String]("industry_name")
  def tenantLocation = column[String]("tenant_location")
  def successProbability = column[Int]("success_probability")
  def accountId = column[Int]("account_id")
  def createdAt = column[LocalDateTime]("created_at")

  /**
   * Default projection mapping columns to the case class.
   *
   * The * method defines how to construct an ActivityLog from
   * database columns and vice versa. The <> operator provides
   * bidirectional mapping using the case class apply/unapply.
   */
  def * = (
    id,
    status,
    userId,
    industryName,
    tenantLocation,
    successProbability,
    accountId,
    createdAt
  ) <> (ActivityLog.apply, ActivityLog.unapply)

/**
 * Table definition for proposals.
 *
 * Note the foreign key relationship to activity_logs.
 */
class ProposalTable(tag: Tag) extends Table[Proposal](tag, "proposals"):

  def id = column[Int]("id", O.PrimaryKey)
  def activityLogId = column[Int]("activity_log_id")
  def lastEntered = column[Boolean]("last_entered")
  def editLocked = column[Boolean]("edit_locked")
  def proposalType = column[String]("proposal_type")
  def createdAt = column[LocalDateTime]("created_at")

  def * = (
    id,
    activityLogId,
    lastEntered,
    editLocked,
    proposalType,
    createdAt
  ) <> (Proposal.apply, Proposal.unapply)

  /**
   * Foreign key constraint to activity_logs.
   *
   * This defines a type-safe relationship between tables.
   * Slick can use this for join queries.
   */
  def activityLog = foreignKey(
    "fk_activity_log",
    activityLogId,
    TableQuery[ActivityLogTable]
  )(_.id, onDelete = ForeignKeyAction.Cascade)

/**
 * TableQuery objects for constructing database queries.
 *
 * These are the entry points for Slick queries.
 * Think of them as "handles" to the tables in the database.
 */
object Tables:
  val activityLogs = TableQuery[ActivityLogTable]
  val proposals = TableQuery[ProposalTable]
