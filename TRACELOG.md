## 0.1.1 - Repository-local Gradle Wrapper

### Task
Add a repository-local Gradle wrapper so Labrinth builds do not depend on a
neighboring project, then finish validation for Phase 0.1.

### Changes
- Added the Gradle 8.10.2 wrapper scripts and bootstrap files.
- Incremented the mod version from `0.1.0` to `0.1.1`.
- Completed the 0.1 checklist in `TASK.md`.
- Preserved the wrapper JAR in Git despite the general JAR ignore rule.

### Implementation
The wrapper uses the existing Gradle 8.10.2 distribution configuration and
the Java 21 toolchain already required by NeoForge 1.21.1. No project code or
read-only reference source was changed.

### Rationale
A repository-local wrapper makes the build reproducible from a fresh checkout
and removes the previous dependency on another repository's wrapper.

### Validation
- `gradlew.bat --version` reported Gradle `8.10.2` and Java `21.0.11`.
- `gradlew.bat build --console=plain --no-daemon` completed successfully.
- Processed metadata reported `The Labrinth 0.1.1 (labrinth)` with Minecraft
  `1.21.1` and NeoForge `21.1.219` dependencies.
- `gradlew.bat runServer` reached `Done (4.558s)` and loaded the mod on a
  dedicated server. The non-interactive dev process was then stopped by its
  exact PID after startup; the intentional termination returned `-1`.

## 0.1.0 - Project Validation

### Task
Complete the first incomplete section under Phase 0 — Project Foundation:
validate and establish the minimum NeoForge 1.21.1 project foundation.

### Changes
- Added the NeoForge ModDev build configuration for Minecraft 1.21.1.
- Added the `labrinth` mod metadata with display name `The Labrinth`.
- Added the common-only `com.labrinthmc.labrinth.TheLabrinth` entry point.
- Added the initial resource pack metadata and ignored generated Gradle/server output.

### Implementation
The build uses NeoForge `21.1.219` and a Java 21 toolchain. Metadata is expanded
from Gradle properties during resource processing so the mod ID, display name,
version, and dependency ranges remain aligned with the build. The entry point
contains no client-only imports, allowing the same class to load on a dedicated
server.

### Rationale
The repository contained documentation and reference material but no editable
mod source or Gradle project. A single common entry point and the smallest
working build configuration establish the validation baseline without starting
the later package, registry, or generation phases.

### Validation
- Java `21.0.11` was detected.
- The processed metadata contained `labrinth`, `The Labrinth`, version `0.1.0`,
  Minecraft `1.21.1`, and NeoForge `21.1.219` dependency ranges.
- `ModernCompanions\gradlew.bat -p R:\Users\Zach\Documents\GitHub\LabrinthMC build --console=plain --no-daemon` completed successfully.
- The dedicated `runServer` task discovered The Labrinth, logged the common
  entry point, reached `Done (7.240s)`, and stopped cleanly.
- No template/example source existed to remove.
- A repository-local Gradle wrapper is still absent because the current
  directory policy does not permit adding wrapper files.
