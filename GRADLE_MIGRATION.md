# Gradle Migration Guide

## ✅ Migration Complete!

The project has been successfully migrated from **sbt** to **Gradle**.

## What Changed

### Build Files

**Removed:**
- `build.sbt` - sbt build configuration
- `project/build.properties` - sbt version
- `project/` directory - sbt plugins

**Added:**
- `build.gradle.kts` - Gradle build configuration (Kotlin DSL)
- `settings.gradle.kts` - Gradle settings
- `gradlew` / `gradlew.bat` - Gradle wrapper scripts
- `gradle/wrapper/` - Gradle wrapper files

### Key Differences

| Feature | sbt | Gradle |
|---------|-----|--------|
| Build file | `build.sbt` | `build.gradle.kts` |
| Compile | `sbt compile` | `./gradlew compileScala` |
| Test | `sbt test` | `./gradlew test` |
| Run | `sbt run` | `./gradlew run` |
| Clean | `sbt clean` | `./gradlew clean` |
| Format | `sbt scalafmt` | `./gradlew spotlessApply` |
| Shell | `sbt` (interactive) | `./gradlew --continuous` |
| Dependencies | Downloads to `~/.ivy2` | Downloads to `~/.gradle` |

### Command Mapping

```bash
# sbt → Gradle equivalents

sbt compile          →  ./gradlew compileScala
sbt test             →  ./gradlew test
sbt run              →  ./gradlew run
sbt clean            →  ./gradlew clean
sbt package          →  ./gradlew jar

# With arguments
sbt "run --help"                    →  ./gradlew run --args="--help"
sbt "run -r 1000 -m jdbc"           →  ./gradlew run --args="-r 1000 -m jdbc"
sbt "testOnly DataGeneratorsSpec"   →  ./gradlew test --tests "DataGeneratorsSpec"

# Interactive mode
sbt                  →  ./gradlew --continuous build
> compile            →  (auto-recompiles on file save)
```

## Advantages of Gradle for This Project

1. **Familiar to you** - You already know Gradle
2. **No separate install** - Gradle wrapper included
3. **Build cache** - Faster incremental builds
4. **IDE integration** - IntelliJ imports Gradle projects easily
5. **Task visualization** - `./gradlew tasks` shows everything

## Gradle Basics for Scala

### Project Structure
Gradle uses the same directory structure as sbt:
```
src/
├── main/
│   ├── scala/
│   └── resources/
└── test/
    └── scala/
```

### Gradle Wrapper
Always use `./gradlew` (not `gradle`) to ensure consistent Gradle version:

```bash
# Unix/Mac
./gradlew build

# Windows
gradlew.bat build
```

### Common Tasks

```bash
# Build everything (compile + test)
./gradlew build

# Just compile
./gradlew compileScala

# Run tests
./gradlew test

# Run application
./gradlew run

# Clean build directory
./gradlew clean

# See all tasks
./gradlew tasks

# Get project info
./gradlew info
```

### Gradle Features You'll Love

1. **Task dependencies** - Gradle knows what to rebuild
2. **Build cache** - Reuses outputs across builds
3. **Parallel execution** - `--parallel` flag
4. **Rich console output** - Progress bars and colors
5. **Detailed diagnostics** - `--info`, `--debug`, `--scan`

### Customizing the Build

The `build.gradle.kts` file is where all configuration lives:

```kotlin
// Add a dependency
dependencies {
    implementation("com.example:library:1.0.0")
}

// Add a custom task
tasks.register("hello") {
    doLast {
        println("Hello from Gradle!")
    }
}

// Configure Scala compiler
tasks.withType<ScalaCompile> {
    scalaCompileOptions.apply {
        additionalParameters = listOf("-feature", "-deprecation")
    }
}
```

## Integration with IDEs

### IntelliJ IDEA

1. Open → Select project directory
2. IntelliJ auto-detects Gradle
3. Click "Import Gradle Project"
4. Wait for dependency download
5. Ready to go!

### VS Code

1. Install "Gradle for Java" extension
2. Open project folder
3. VS Code detects `build.gradle.kts`
4. Use Gradle sidebar for tasks

## Troubleshooting

### Gradle Daemon Issues

```bash
# Stop all Gradle daemons
./gradlew --stop

# Run without daemon (slower but cleaner)
./gradlew build --no-daemon
```

### Dependency Issues

```bash
# Refresh dependencies
./gradlew build --refresh-dependencies

# See dependency tree
./gradlew dependencies
```

### Build Cache Issues

```bash
# Clean everything
./gradlew clean

# Disable cache for one build
./gradlew build --no-build-cache
```

## Learning Resources

- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Gradle Scala Plugin](https://docs.gradle.org/current/userguide/scala_plugin.html)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)

## Still Missing sbt?

If you prefer sbt, you can switch back:
1. Restore `build.sbt` from git history
2. Delete `build.gradle.kts`, `settings.gradle.kts`, `gradlew*`
3. Delete `.gradle/` directory

But give Gradle a week - you might like it! 😊
