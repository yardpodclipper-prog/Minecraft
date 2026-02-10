# Cobblemon GTS Tracker

Client-side Fabric mod that parses GTS listings from chat and stores snapshots locally for price/disappearance analysis.

## Compatibility
- **JDK:** 21
- **Supported Minecraft version:** 1.21.1
- **Official build system:** Gradle (Fabric Loom)

## Compatibility Matrix

| Target Stack | Minecraft | Fabric Loader | Fabric API | Cobblemon |
| --- | --- | --- | --- | --- |
| CobbleGalaxy 1.7.3 | 1.21.1 | 0.18.4 | 0.116.8+1.21.1 | 1.7.3+1.21.1 |

> If your loader/build files target different versions, update this README to match.


## Canonical runtime architecture

`GTSTrackerMod` is the only supported runtime entrypoint and initializes the production ingestion pipeline in this order:

1. `config.ConfigManager` loads `ConfigModel`.
2. `database.DatabaseManager` initializes SQLite schema and connection.
3. `ingest.ListingIngestionService` ingests parsed listings into the DB.
4. `chat.GTSChatMonitor` listens to incoming chat and forwards GTS lines into ingestion.
5. `ui.CommandHandler` registers commands that interact with the same runtime services.

The canonical parsing + persistence path is:

`chat.GTSMessageParser` -> `ingest.ListingIngestionService` -> `database.DatabaseManager`

Legacy duplicate classes that previously existed in `com.yourname.gtstracker` were removed to avoid ambiguity. Keep new parsing, ingestion, and persistence changes on the package-scoped runtime path above.

## Commands

The mod registers both namespaced and legacy command roots:

- `/gtstracker status`
- `/gtstracker ingesttest <message>`
- `/gtstracker gui`
- `/gts ...` (legacy alias)

## Build and test

### Build

```bash
./gradlew build
```

> Maven is not used for this mod runtime/build pipeline.

### Java tests

```bash
./gradlew test
```

### Python tests

```bash
pytest -q
```

### Run in development

```bash
./gradlew runClient
```


## Current verification status (headless container)

Automated checks currently pass:

- `./gradlew clean test`
- `./gradlew build`
- `pytest -q`

### Release jar and validation

#### Artifact differences

- `build/devlibs/*-dev.jar` is the **development jar** (readable names/unmapped for production) and is primarily for local dev/runtime tooling.
- `build/libs/*.jar` is the **release/remapped jar** expected for real client deployment.
- In CI, if the remapped jar is invalid/missing, the workflow promotes a valid `*-dev.jar` into `build/libs/*.jar` as a fallback so uploads still contain runtime classes/resources.

#### Exact release command

Local release build:

```bash
./gradlew clean build
```

CI release build command (from `.github/workflows/build-jar.yml`):

```bash
./gradlew clean build -x test
```

Expected outputs after a successful build:

- `build/libs/gtstracker-<version>.jar` exists and is non-empty.
- `verifyReleaseJar` passes (it runs after `build` and fails if the release jar is missing/empty).
- Workflow artifact `gtstracker-jar` uploads successfully from the resolved jar path.

#### Inspecting jar contents

Use `jar tf` to inspect either candidate jar:

```bash
jar tf build/libs/gtstracker-<version>.jar
jar tf build/devlibs/gtstracker-<version>-dev.jar
```

At minimum, confirm entries like:

- `fabric.mod.json`
- `com/yourname/gtstracker/GTSTrackerMod.class`

#### Common failure modes and fixes

- **Tiny jar (usually <10 KB):** jar likely contains metadata only or wrong output was selected.
  - Fix: rerun `./gradlew clean build`, then compare `build/libs` vs `build/devlibs` sizes and inspect both with `jar tf`.
- **Entrypoint crash on startup:** release jar may be missing required classes/resources.
  - Fix: inspect `run/logs/latest.log`, then validate jar contains `GTSTrackerMod.class` + `fabric.mod.json`; rebuild and retest in a clean profile.
- **Unremapped classes in release artifact:** `build/libs/*.jar` may be invalid while `*-dev.jar` has classes.
  - Fix: run `./gradlew remapJar build`, and if CI still detects bad remap output, use the promoted fallback jar while investigating Loom/remap environment issues.

#### CI workflow and fast diagnostics

- CI workflow: [`.github/workflows/build-jar.yml`](.github/workflows/build-jar.yml)
- Validation tasks to run locally when diagnosing CI failures:
  - `./gradlew clean build`
  - `./gradlew remapJar`
  - `./gradlew verifyReleaseJar`
  - `jar tf build/libs/gtstracker-<version>.jar`


## Download-ready jar from GitHub (no binary committed)

This repository does not commit `.jar` binaries directly (to avoid Git/GitHub binary diff limitations).
Use one of these download paths instead:

1. **Actions artifact** (every push/manual workflow run):
   - Open the **Build Jar** workflow run.
   - Download the `gtstracker-jar` artifact.
2. **Release asset** (recommended for end users):
   - Create/push a tag like `v0.1.0`.
   - Download `gtstracker-0.1.0.jar` from the GitHub Release assets.


## Dependency packaging policy

Runtime dependency packaging is intentionally split into **embedded mod dependencies** and **external runtime dependencies**:

- **Embedded (inside the mod jar):** Fabric/Cobblemon mod dependencies handled by Loom remapping for mod runtime compatibility.
- **External (not nested in the mod jar):** `org.xerial:sqlite-jdbc`.

`sqlite-jdbc` is loaded from the runtime classpath (dependency manager/modpack launcher) instead of being nested with Loom `include`. This avoids invalid-semver include processing warnings on `processIncludeJars` and keeps release packaging deterministic.

Release packaging policy:

1. `jar` builds the canonical dev artifact (`build/devlibs/...-dev.jar`) with compiled classes/resources.
2. `remapJar` still runs and is preferred when it contains required runtime entries.
3. `prepareReleaseJar` verifies remap output and falls back to the dev jar if remap output is stripped/missing runtime entries.
4. `verifyReleaseJar` enforces final release-jar validity (`fabric.mod.json` + `GTSTrackerMod.class`) so no build step can silently strip/overwrite deployable output.

## Runtime output locations

When running from Gradle, runtime artifacts are typically written under `run/`:

- Logs: `run/logs/latest.log`
- Database: `run/config/gtstracker/gtstracker.db`
- On startup the mod now logs environment compatibility information (Minecraft/Fabric Loader versions + Cobblemon loaded state) to help production debugging.

### Production diagnostics

If startup or GUI fails in production, GTSTracker now emits explicit error logs during mod initialization and GUI launch, with player-facing fallback messaging for GUI failures. Check `latest.log` first when diagnosing issues.

## Release checklist for stable modpack usage

- [ ] Jar built from clean repo (`./gradlew clean build`)
- [ ] Loads in a clean client profile with only Fabric Loader + Fabric API + this mod
- [ ] Loads in CobbleGalaxy modstack without command/keybinding conflicts
- [ ] `/gtstracker status` works and DB initializes
- [ ] `/gtstracker gui` opens the Bloomberg screen without exceptions
- [ ] No GTSTracker initialization/GUI error entries in `latest.log`
- [ ] No startup exceptions in `latest.log`

- **Logs:** `run/logs/latest.log` (and related files in `run/logs/`)
- **DB files:** any runtime-created database file (for example `*.db`) will appear in `run/` unless configured otherwise in code.

## Known-good test profile versions

- **Minecraft:** `1.21.1`
- **Fabric Loader:** `0.18.4`
- **Fabric API:** `0.116.8+1.21.1`
- **Cobblemon:** `1.7.3+1.21.1`
- **Cobblemon GTS Tracker:** `0.1.0`
- **Client commands:** `/gts ...` and `/gtstracker ...` are both registered and point to the same handlers.

> Verification note: in this headless CI/container environment, client bootstrap reaches mod initialization (including `Cobblemon GTS Tracker initialized.`) but crashes before interactive command entry because no GLFW display is available.
