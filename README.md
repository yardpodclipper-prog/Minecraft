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

### Release jar

Run:

```bash
./gradlew clean build
```

Use this artifact for deployment/testing:

- `build/libs/gtstracker-0.1.0.jar` (remapped release jar)

Optional verification:

```bash
jar tf build/libs/gtstracker-0.1.0.jar | head
```

The build now includes `verifyReleaseJar`, which validates that the release jar contains required runtime entries (`fabric.mod.json` and `GTSTrackerMod.class`) before `check` passes.

## Runtime output locations

When running from Gradle, runtime artifacts are typically written under `run/`:

- Logs: `run/logs/latest.log`
- Database: `run/config/gtstracker/gtstracker.db`

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
