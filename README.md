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

Primary artifact for live use:

- `build/libs/gtstracker-0.1.0.jar` (packaged with classes/resources from the built jar)

Validation command:

```bash
jar tf build/libs/gtstracker-0.1.0.jar
```

You should see classes/resources such as `com/yourname/gtstracker/GTSTrackerMod.class` and `fabric.mod.json`.

> Build note: the Loom `remapJar` task remains environment-sensitive in this container, so the `liveJar` packaging step now guarantees a class-containing deliverable at the canonical jar name for deployment testing.

### What is still needed before production rollout

1. Verify in a real game client with display support (`./gradlew runClient`) using the pinned stack versions below.
2. Open `/gtstracker gui` and verify no runtime exceptions are written while interacting with panels/widgets.
3. Confirm `/gtstracker status` initializes DB and writes expected runtime files (`run/logs/latest.log`, `run/config/gtstracker/gtstracker.db`).
4. Test in both a clean Fabric profile and the full CobbleGalaxy modpack profile to confirm compatibility.
Use this artifact for deployment/testing:

- `build/libs/gtstracker-0.1.0.jar` (remapped release jar)

Optional verification:

```bash
jar tf build/libs/gtstracker-0.1.0.jar | head
```

The build now includes `verifyReleaseJar`, which validates that the release jar contains required runtime entries (`fabric.mod.json` and `GTSTrackerMod.class`) before `check` passes.
Use this artifact for immediate testing/deployment:

- `build/libs/gtstracker-0.1.0.jar` (produced by Loom `remapJar`; build now fails if this artifact is missing/invalid).

Optional debug artifact:

- `build/libs/gtstracker-0.1.0-test.jar`

### What is still needed for final live verification

1. Verify in a real game client with display support (`./gradlew runClient`) using the pinned stack versions above.
2. Confirm `/gtstracker status` initializes DB and writes expected runtime files (`run/logs/latest.log`, `run/config/gtstracker/gtstracker.db`).
3. Run `/gtstracker gui` to verify GUI opening/rendering on your target modpack profile.
4. Re-run `./gradlew clean build` and verify final artifact contents with: `jar tf build/libs/<jar-name>.jar`.

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
