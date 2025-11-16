package com.fakedatagen.domain

import java.time.LocalDateTime

/**
 * Domain models representing our database entities.
 *
 * These case classes use Scala 3 syntax and provide type-safe
 * representations of our database tables.
 */

/**
 * Represents an activity log entry tracking user actions.
 *
 * @param id Unique identifier for the activity
 * @param status Current status of the activity
 * @param userId ID of the user who performed the activity
 * @param industryName Name of the industry associated with this activity
 * @param tenantLocation Geographic location of the tenant
 * @param successProbability Probability of success (0-100)
 * @param accountId Associated account identifier
 * @param createdAt Timestamp when the activity was created
 */
case class ActivityLog(
  id: Int,
  status: String,
  userId: Int,
  industryName: String,
  tenantLocation: String,
  successProbability: Int,
  accountId: Int,
  createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Represents a proposal linked to an activity log.
 *
 * @param id Unique identifier for the proposal
 * @param activityLogId Reference to the associated activity log
 * @param lastEntered Flag indicating if this was the last proposal entered
 * @param editLocked Flag indicating if the proposal is locked for editing
 * @param proposalType Type/category of the proposal
 * @param createdAt Timestamp when the proposal was created
 */
case class Proposal(
  id: Int,
  activityLogId: Int,
  lastEntered: Boolean,
  editLocked: Boolean,
  proposalType: String,
  createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Configuration for database connection parameters.
 *
 * @param url JDBC connection URL
 * @param driver JDBC driver class name
 * @param user Database username
 * @param password Database password
 */
case class DatabaseConfig(
  url: String,
  driver: String,
  user: String,
  password: String
)

/**
 * Configuration for the data generation process.
 *
 * @param recordCount Number of records to generate
 * @param batchSize Size of each batch for insertion
 * @param useJdbc Whether to use JDBC implementation
 * @param useSlick Whether to use Slick implementation
 */
case class GenerationConfig(
  recordCount: Int,
  batchSize: Int,
  useJdbc: Boolean,
  useSlick: Boolean
)
