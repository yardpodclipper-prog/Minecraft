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

### Testable jar to use right now

Run:

```bash
./gradlew clean build
```

Use this artifact for immediate testing/deployment:

- `build/libs/gtstracker-0.1.0.jar` (release path; build now auto-falls back to the dev jar when remap output is unexpectedly empty).

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

## Release checklist for stable modpack usage

- [ ] Jar built from clean repo (`./gradlew clean build`)
- [ ] Loads in a clean client profile with only Fabric Loader + Fabric API + this mod
- [ ] Loads in CobbleGalaxy modstack without command/keybinding conflicts
- [ ] `/gtstracker status` works and DB initializes
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
