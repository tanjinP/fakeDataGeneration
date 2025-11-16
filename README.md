# Fake Data Generation - Scala Learning Project

A comprehensive Scala 3 project demonstrating type-safe database operations with both JDBC and Slick (Functional Relational Mapping). Generate realistic fake data for PostgreSQL databases while learning modern Scala patterns.

## Learning Objectives

This project teaches:

- **Scala 3 Features**: Modern syntax, case classes, enums, extension methods
- **JDBC Fundamentals**: Raw database connections, PreparedStatements, batch operations
- **Slick FRM**: Type-safe queries, table definitions, functional database access
- **Type Safety**: Compile-time guarantees for database operations
- **Functional Programming**: Either/Try for error handling, immutable data structures
- **Data Generation**: Property-based testing concepts with ScalaCheck Faker
- **CLI Development**: Argument parsing with Decline library
- **Project Structure**: Clean architecture, separation of concerns

## Project Structure

```
src/
├── main/
│   ├── scala/com/fakedatagen/
│   │   ├── domain/          # Domain models (case classes)
│   │   ├── jdbc/            # JDBC implementation
│   │   ├── slick/           # Slick FRM implementation
│   │   ├── generators/      # Fake data generators
│   │   ├── config/          # Configuration loading
│   │   └── Main.scala       # CLI application
│   └── resources/
│       ├── application.conf # Database configuration
│       └── logback.xml      # Logging configuration
└── test/
    └── scala/               # Unit tests
```

## Prerequisites

- **Java 21 LTS** (check with `java -version`)
  - Recommended: Use [mise](https://mise.jdx.dev/) - see `.mise.toml` in project root
  - Alternative: Download from [Adoptium](https://adoptium.net/)
- **Gradle 8.11+** (included via wrapper - no separate install needed!)
- **PostgreSQL 17**
  - macOS: `brew install postgresql@17`
  - Linux: `sudo apt install postgresql-17`

## Quick Start

### 0. (Optional) Install Java with mise

If you use [mise](https://mise.jdx.dev/) for tool management:

```bash
# Install mise (if not already installed)
curl https://mise.run | sh

# Install Java 21 and activate it for this project
mise install

# Verify Java version
java -version  # Should show Java 21
```

Mise will automatically activate Java 21 when you `cd` into this directory!

### 1. Set Up PostgreSQL

**Start PostgreSQL service:**

```bash
# macOS (Homebrew)
brew services start postgresql@17

# Linux (systemd)
sudo systemctl start postgresql
```

**Run the setup script:**

```bash
./scripts/setup-db.sh
```

The script will:
- Create the `fakedata` database
- Apply the schema
- Configure application settings
- Test the connection

### 2. Compile the Project

```bash
# Download dependencies and compile
./gradlew build

# This may take a few minutes on first run
# The Gradle wrapper will automatically download Gradle if needed
```

### 3. Run Tests

```bash
# Run all unit tests
./gradlew test

# Run with detailed output
./gradlew test --info

# Run a specific test class
./gradlew test --tests "com.fakedatagen.DataGeneratorsSpec"
```

### 4. Run the Application

#### Test Database Connection

```bash
./gradlew testConnection
```

#### Generate Data with Default Settings (10,000 records, both JDBC and Slick)

```bash
./gradlew run
```

#### Generate Data with Custom Settings

```bash
# Generate 1,000 records using only JDBC
./gradlew run --args="--records 1000 --method jdbc"

# Generate 50,000 records using only Slick
./gradlew run --args="--records 50000 --method slick"

# Generate 5,000 records with custom batch size
./gradlew run --args="--records 5000 --batch-size 1000"

# Show help
./gradlew run --args="--help"
```

## Available CLI Options

```
--records, -r       Number of records to generate (default: 10000)
--batch-size, -b    Batch size for inserts (default: 5000)
--method, -m        Database access method: jdbc, slick, or both (default: both)
--test-connection   Test database connection and exit
```

## Database Commands

### Manual Schema Setup

If you need to set up the database manually:

```bash
# Connect to PostgreSQL
psql -U $(whoami) postgres

# Create database and schema
CREATE DATABASE fakedata;
\c fakedata
\i database/schema.sql
```

### Inspect the Data

```bash
# Connect to the database
psql -U $(whoami) fakedata

# View record counts
SELECT 'activity_logs' as table_name, COUNT(*) FROM activity_logs
UNION ALL
SELECT 'proposals', COUNT(*) FROM proposals;

# View sample data
SELECT * FROM activity_logs LIMIT 10;
SELECT * FROM proposals LIMIT 10;

# View a join query (like Slick does)
SELECT a.id, a.status, p.proposal_type
FROM activity_logs a
JOIN proposals p ON p.activity_log_id = a.id
LIMIT 10;

# Exit
\q
```

### Reset the Database

```bash
# Clear all data (keeps schema)
./scripts/reset-db.sh

# Or drop and recreate completely
./scripts/setup-db.sh
```

## Build Commands

### Compile

```bash
# Compile main sources
./gradlew compileScala

# Compile tests
./gradlew compileTestScala

# Clean and recompile
./gradlew clean build
```

### Format Code

```bash
# Check formatting
./gradlew spotlessCheck

# Format all code
./gradlew spotlessApply
```

### Package

```bash
# Create a JAR file
./gradlew jar

# Create a distribution with all dependencies
./gradlew installDist
# Output will be in build/install/fakeDataGeneration/
```

### Run in Different Modes

```bash
# Run with specific JVM options
./gradlew run -Dorg.gradle.jvmargs="-Xmx2G"

# Run in background
./gradlew run --args="--records 100000" &

# Run with environment variables
DB_URL="jdbc:postgresql://localhost:5432/fakedata" ./gradlew run
```

## Understanding the Code

### JDBC vs Slick Comparison

#### JDBC Approach (jdbc/JdbcRepository.scala)

**Pros:**
- Full control over SQL
- No abstraction overhead
- Easy to debug with SQL logs
- Familiar to developers from other languages

**Cons:**
- Manual resource management
- SQL injection risk if not using PreparedStatements
- No compile-time query validation
- Verbose boilerplate

**Example:**
```scala
val sql = "INSERT INTO activity_logs (id, status, ...) VALUES (?, ?, ...)"
stmt.setInt(1, activity.id)
stmt.setString(2, activity.status)
// ... manual parameter binding
stmt.executeBatch()
```

#### Slick Approach (slick/SlickRepository.scala)

**Pros:**
- Type-safe queries checked at compile-time
- Automatic SQL generation
- Composable queries
- Connection pooling built-in
- Less boilerplate

**Cons:**
- Learning curve for the API
- Some abstraction overhead
- Complex queries can be tricky
- Generated SQL may not always be optimal

**Example:**
```scala
val action = activityLogs ++= activities
db.run(action)
// Slick generates the SQL automatically!
```

### Key Files to Study

1. **domain/Models.scala** - Scala 3 case classes, documentation
2. **jdbc/JdbcRepository.scala** - Raw JDBC with detailed comments
3. **slick/SlickTables.scala** - Type-safe table definitions
4. **slick/SlickRepository.scala** - Functional database access
5. **generators/DataGenerators.scala** - Property-based data generation
6. **Main.scala** - CLI argument parsing, program flow

## Extending the Project

### Add a New Table

1. Define the case class in `domain/Models.scala`
2. Create JDBC insert method in `jdbc/JdbcRepository.scala`
3. Create Slick table definition in `slick/SlickTables.scala`
4. Add generator in `generators/DataGenerators.scala`
5. Update schema in `database/schema.sql`
6. Wire it up in `Main.scala`

### Add More Realistic Data

Edit `generators/DataGenerators.scala` to use more Faker generators:

```scala
import faker._

// Use built-in Faker data
val nameGen = Gen.const(name.firstName())
val emailGen = Gen.const(internet.emailAddress())
val companyGen = Gen.const(company.name())
```

### Add Query Examples

Add methods to the repositories to demonstrate different query types:

```scala
// In JdbcRepository
def findByStatus(status: String): List[ActivityLog] = ???

// In SlickRepository
def findByStatus(status: String): Either[String, List[ActivityLog]] =
  val query = activityLogs.filter(_.status === status).result
  // ... execute query
```

## Troubleshooting

### Database Connection Errors

```bash
# Check if PostgreSQL is running
brew services list | grep postgresql

# Check PostgreSQL status
psql -U $(whoami) postgres -c "SELECT version();"

# Restart PostgreSQL
brew services restart postgresql@17
```

### Compilation Errors

```bash
# Clean build artifacts
sbt clean

# Update dependencies
sbt update

# Check Scala version
sbt scalaVersion
```

### Out of Memory Errors

```bash
# Increase JVM heap size
sbt -J-Xmx4G run

# Or reduce batch size
sbt "run --batch-size 1000"
```

## Performance Tips

- **Batch Size**: 5,000-10,000 records per batch is optimal for PostgreSQL
- **Connection Pooling**: Slick uses HikariCP automatically
- **Indexing**: The schema includes indexes on foreign keys and commonly queried columns
- **Disable Auto-commit**: Both implementations disable auto-commit for better performance

## Additional Resources

- [Scala 3 Documentation](https://docs.scala-lang.org/scala3/)
- [Slick Documentation](https://scala-slick.org/doc/3.5.0/)
- [JDBC Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/)
- [ScalaCheck User Guide](https://www.scalacheck.org/)
- [Decline Library](https://ben.kirw.in/decline/)

## License

MIT License - feel free to use this for learning and teaching!

## Related Projects

This project was created to support database provisioning for an [ETL project](https://github.com/tanjinP/zero2etl).
