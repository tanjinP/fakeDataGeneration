package com.fakedatagen.generators

import com.fakedatagen.domain.{ActivityLog, Proposal}
import org.scalacheck.{Arbitrary, Gen}
import faker.*
import java.time.LocalDateTime

/**
 * Type-safe data generators using ScalaCheck and Faker.
 *
 * This demonstrates how to create realistic fake data with
 * proper types and constraints for database population.
 */
object DataGenerators:

  // Import faker instances for generating realistic data
  given Faker = Faker.default

  /** Generates a random status string from predefined options */
  val statusGen: Gen[String] = Gen.oneOf(
    "pending",
    "in_progress",
    "completed",
    "failed",
    "cancelled"
  )

  /** Generates a random industry name using Faker */
  val industryGen: Gen[String] = Gen.oneOf(
    "Technology",
    "Healthcare",
    "Finance",
    "Manufacturing",
    "Retail",
    "Education",
    "Real Estate",
    "Transportation"
  )

  /** Generates a random city location */
  val locationGen: Gen[String] = Gen.oneOf(
    "New York, NY",
    "Los Angeles, CA",
    "Chicago, IL",
    "Houston, TX",
    "Phoenix, AZ",
    "Philadelphia, PA",
    "San Antonio, TX",
    "San Diego, CA",
    "Dallas, TX",
    "San Jose, CA",
    "Austin, TX",
    "Seattle, WA",
    "Denver, CO",
    "Boston, MA",
    "Miami, FL",
    "Atlanta, GA"
  )

  /** Generates a random proposal type */
  val proposalTypeGen: Gen[String] = Gen.oneOf(
    "Standard",
    "Premium",
    "Enterprise",
    "Custom",
    "Trial"
  )

  /**
   * Generates a single ActivityLog with realistic data.
   *
   * @param id The ID to assign to this activity log
   * @return Gen[ActivityLog] generator for an activity log
   */
  def activityLogGen(id: Int): Gen[ActivityLog] = for {
    status <- statusGen
    userId <- Gen.choose(1, 1000)
    industry <- industryGen
    location <- locationGen
    probability <- Gen.choose(0, 100)
    accountId <- Gen.choose(1, 500)
  } yield ActivityLog(
    id = id,
    status = status,
    userId = userId,
    industryName = industry,
    tenantLocation = location,
    successProbability = probability,
    accountId = accountId,
    createdAt = LocalDateTime.now().minusDays(Gen.choose(0, 365).sample.getOrElse(0))
  )

  /**
   * Generates a single Proposal linked to an ActivityLog.
   *
   * @param id The ID to assign to this proposal
   * @param activityLogId The activity log this proposal relates to
   * @return Gen[Proposal] generator for a proposal
   */
  def proposalGen(id: Int, activityLogId: Int): Gen[Proposal] = for {
    lastEntered <- Gen.prob(0.2) // 20% chance of being true
    editLocked <- Gen.prob(0.3) // 30% chance of being locked
    propType <- proposalTypeGen
  } yield Proposal(
    id = id,
    activityLogId = activityLogId,
    lastEntered = lastEntered,
    editLocked = editLocked,
    proposalType = propType,
    createdAt = LocalDateTime.now().minusDays(Gen.choose(0, 365).sample.getOrElse(0))
  )

  /**
   * Generates a batch of ActivityLog records.
   *
   * This demonstrates efficient batch generation using Gen.sequence.
   *
   * @param startId Starting ID for the batch
   * @param count Number of records to generate
   * @return List of ActivityLog instances
   */
  def generateActivityLogs(startId: Int, count: Int): List[ActivityLog] =
    val generators = (startId until startId + count).map(activityLogGen)
    Gen.sequence[List[ActivityLog], ActivityLog](generators).sample.getOrElse(List.empty)

  /**
   * Generates a batch of Proposal records.
   *
   * @param startId Starting ID for the batch
   * @param count Number of records to generate
   * @param activityLogIdMin Minimum valid activity log ID to reference
   * @param activityLogIdMax Maximum valid activity log ID to reference
   * @return List of Proposal instances
   */
  def generateProposals(
    startId: Int,
    count: Int,
    activityLogIdMin: Int = 1,
    activityLogIdMax: Int = Int.MaxValue
  ): List[Proposal] =
    val generators = (startId until startId + count).map { id =>
      // Link each proposal to a random activity log within valid range
      val range = activityLogIdMax - activityLogIdMin + 1
      val activityLogId =
        if range > 0 then activityLogIdMin + scala.util.Random.nextInt(range)
        else activityLogIdMin
      proposalGen(id, activityLogId)
    }
    Gen.sequence[List[Proposal], Proposal](generators).sample.getOrElse(List.empty)

  /**
   * Generates data in chunks for efficient batch processing.
   *
   * This is crucial for avoiding memory issues with large datasets.
   *
   * @param totalCount Total number of records to generate
   * @param batchSize Size of each batch
   * @param generator Function to generate a batch
   * @return Iterator of batches
   */
  def generateInBatches[T](
    totalCount: Int,
    batchSize: Int,
    generator: (Int, Int) => List[T]
  ): Iterator[List[T]] =
    (0 until totalCount by batchSize).iterator.map { startId =>
      val count = Math.min(batchSize, totalCount - startId)
      generator(startId + 1, count)
    }
