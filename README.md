# Cobblemon GTS Tracker

Client-side Fabric mod that parses GTS listings from chat and stores snapshots locally for price/disappearance analysis.

## Compatibility
- **JDK:** 21
- **Supported Minecraft version:** 1.21.1

| Component | Supported version |
| --- | --- |
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.9+ |
| Fabric API | 0.116.7+1.21.1 |
| Java | 21 |

> For CobbleGalaxy deployments: verify your exact modpack's Minecraft/Fabric/Cobblemon versions match the table above before distributing this jar.

## Drag-and-drop installation (Modrinth/CobbleGalaxy users)

1. Install Fabric Loader for the exact Minecraft version above.
2. Ensure Fabric API is present in your `mods/` folder.
3. Download the `gtstracker-<version>.jar` release artifact.
4. Drop the jar into your client `mods/` folder.
5. Launch client and verify startup log contains `Cobblemon GTS Tracker initialized.`

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
- **Fabric Loader:** `0.16.9`
- **Fabric API:** `0.116.7+1.21.1`
- **Cobblemon GTS Tracker:** `0.1.0`
- **Client commands:** `/gts ...` and `/gtstracker ...` are both registered and point to the same handlers.

> Verification note: in this headless CI/container environment, client bootstrap reaches mod initialization (including `Cobblemon GTS Tracker initialized.`) but crashes before interactive command entry because no GLFW display is available.
