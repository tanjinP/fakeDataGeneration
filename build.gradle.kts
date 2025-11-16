plugins {
    scala
    application
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.fakedatagen"
version = "0.2.0"

repositories {
    mavenCentral()
}

// Scala version configuration
val scalaVersion = "3.5.2"
val scalaLibrary = "org.scala-lang:scala3-library_3:$scalaVersion"

dependencies {
    // Scala 3 standard library
    implementation(scalaLibrary)

    // Database
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.typesafe.slick:slick_3:3.5.2")
    implementation("com.typesafe.slick:slick-hikaricp_3:3.5.2")

    // Fake data generation
    implementation("io.github.etspaceman:scalacheck-faker_3:8.0.3")

    // Configuration
    implementation("com.typesafe:config:1.4.3")

    // CLI argument parsing
    implementation("com.monovore:decline_3:2.4.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("com.typesafe.scala-logging:scala-logging_3:3.9.5")

    // Testing
    testImplementation("org.scalatest:scalatest_3:3.2.19")
    testImplementation("org.scalatestplus:scalacheck-1-18_3:3.2.19.0")
    testImplementation("org.scalatestplus:junit-5-10_3:3.2.19.0")
    testRuntimeOnly("org.scala-lang.modules:scala-xml_3:2.3.0")
    testRuntimeOnly("com.vladsch.flexmark:flexmark-all:0.64.8")
}

application {
    mainClass.set("com.fakedatagen.Main")
}

tasks.withType<ScalaCompile> {
    scalaCompileOptions.apply {
        additionalParameters = listOf(
            "-encoding", "utf8",
            "-feature",
            "-unchecked",
            "-deprecation"
            // Note: -Xfatal-warnings removed for Gradle compatibility
        )
    }
}

// Configure Spotless for code formatting (similar to scalafmt)
spotless {
    scala {
        scalafmt("3.8.3").configFile(".scalafmt.conf")
    }
}

// Custom task to run with arguments easily
tasks.register("runWithArgs", JavaExec::class) {
    group = "application"
    description = "Run the application with custom arguments"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.fakedatagen.Main")

    // Pass system properties or use defaults
    args = System.getProperty("runArgs", "").split(" ").filter { it.isNotEmpty() }
}

// Task to test database connection
tasks.register("testConnection", JavaExec::class) {
    group = "application"
    description = "Test database connection"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.fakedatagen.Main")
    args = listOf("--test-connection")
}

// Configure test output
tasks.test {
    // Use JUnit Platform for ScalaTest integration
    useJUnitPlatform {
        includeEngines("scalatest")
    }

    testLogging {
        // Show test events
        events("passed", "skipped", "failed", "standardOut", "standardError")

        // Show detailed output
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // Show individual test results
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

        // Show test names as they run
        displayGranularity = 2
    }

    // Print summary after tests
    afterSuite(KotlinClosure2({ desc: TestDescriptor, result: TestResult ->
        if (desc.parent == null) { // Top-level suite
            println("\n${"\u001B[32m"}Test Results:${"\u001B[0m"}")
            println("  Tests run: ${result.testCount}")
            println("  ${"\u001B[32m"}Passed: ${result.successfulTestCount}${"\u001B[0m"}")
            println("  ${"\u001B[33m"}Skipped: ${result.skippedTestCount}${"\u001B[0m"}")
            println("  ${"\u001B[31m"}Failed: ${result.failedTestCount}${"\u001B[0m"}")
            println("  Duration: ${(result.endTime - result.startTime) / 1000.0}s")

            if (result.failedTestCount > 0) {
                println("\n${"\u001B[31m"}✗ TESTS FAILED${"\u001B[0m"}")
            } else {
                println("\n${"\u001B[32m"}✓ ALL TESTS PASSED${"\u001B[0m"}")
            }
        }
    }))
}

// Ensure resources are available
sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

// Handle duplicate resources (e.g., logback.xml from dependencies)
tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Print helpful message
tasks.register("info") {
    group = "help"
    description = "Display project information and useful commands"

    doLast {
        println("""
            |
            |========================================
            |  Fake Data Generation - Gradle Build
            |========================================
            |
            |Useful commands:
            |
            |  ./gradlew build              - Compile and run tests
            |  ./gradlew run                - Run with defaults
            |  ./gradlew testConnection     - Test DB connection
            |  ./gradlew test               - Run tests
            |  ./gradlew spotlessApply      - Format code
            |  ./gradlew clean build        - Clean and rebuild
            |
            |Run with custom arguments:
            |  ./gradlew run --args="--records 1000 --method jdbc"
            |  ./gradlew run --args="--help"
            |
            |See README.md for more details.
            |========================================
            |
        """.trimMargin())
    }
}
