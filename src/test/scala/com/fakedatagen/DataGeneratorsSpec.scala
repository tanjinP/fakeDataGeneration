package com.fakedatagen

import com.fakedatagen.generators.DataGenerators
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Unit tests for data generators.
 *
 * These tests verify that:
 * - Generators produce the correct number of records
 * - Generated data meets type constraints
 * - Batch generation works correctly
 */
class DataGeneratorsSpec extends AnyFlatSpec with Matchers:

  "ActivityLog generator" should "generate the correct number of records" in {
    val activities = DataGenerators.generateActivityLogs(1, 100)
    activities.size shouldBe 100
  }

  it should "generate records with sequential IDs starting from startId" in {
    val activities = DataGenerators.generateActivityLogs(50, 10)
    activities.head.id shouldBe 50
    activities.last.id shouldBe 59
  }

  it should "generate valid success probability values (0-100)" in {
    val activities = DataGenerators.generateActivityLogs(1, 100)
    activities.foreach { activity =>
      activity.successProbability should (be >= 0 and be <= 100)
    }
  }

  it should "generate non-empty status and industry values" in {
    val activities = DataGenerators.generateActivityLogs(1, 100)
    activities.foreach { activity =>
      activity.status should not be empty
      activity.industryName should not be empty
      activity.tenantLocation should not be empty
    }
  }

  "Proposal generator" should "generate the correct number of records" in {
    val proposals = DataGenerators.generateProposals(1, 50, 1, 100)
    proposals.size shouldBe 50
  }

  it should "generate records with sequential IDs" in {
    val proposals = DataGenerators.generateProposals(100, 10, 1, 100)
    proposals.head.id shouldBe 100
    proposals.last.id shouldBe 109
  }

  it should "link to valid activity log IDs" in {
    val minActivityLogId = 1
    val maxActivityLogId = 50
    val proposals = DataGenerators.generateProposals(1, 50, minActivityLogId, maxActivityLogId)

    proposals.foreach { proposal =>
      proposal.activityLogId should (be >= minActivityLogId and be <= maxActivityLogId)
    }
  }

  it should "generate non-empty proposal types" in {
    val proposals = DataGenerators.generateProposals(1, 100)
    proposals.foreach { proposal =>
      proposal.proposalType should not be empty
    }
  }

  "Batch generator" should "generate correct number of batches" in {
    val totalCount = 1000
    val batchSize = 250
    val batches = DataGenerators
      .generateInBatches(
        totalCount,
        batchSize,
        DataGenerators.generateActivityLogs
      )
      .toList

    batches.size shouldBe 4
    batches.foreach(_.size shouldBe batchSize)
  }

  it should "handle non-divisible counts correctly" in {
    val totalCount = 1000
    val batchSize = 300
    val batches = DataGenerators
      .generateInBatches(
        totalCount,
        batchSize,
        DataGenerators.generateActivityLogs
      )
      .toList

    batches.size shouldBe 4
    batches.take(3).foreach(_.size shouldBe batchSize)
    batches.last.size shouldBe 100 // Remainder
  }

  it should "maintain sequential IDs across batches" in {
    val batches = DataGenerators
      .generateInBatches(
        100,
        30,
        DataGenerators.generateActivityLogs
      )
      .toList

    // First batch should start at ID 1
    batches.head.head.id shouldBe 1

    // Each batch should continue from where the previous left off
    batches(0).last.id shouldBe 30
    batches(1).head.id shouldBe 31
    batches(1).last.id shouldBe 60
  }
