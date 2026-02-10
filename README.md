# Minecraft Mod Development

## Requirements

- **JDK:** 21
- **Supported Minecraft version:** 1.21.1

> If your loader/build files target different versions, update this README to match.

## Commands

### Build

```bash
./gradlew build
```

### Tests

```bash
./gradlew test
```

### Run in development

```bash
./gradlew runClient
```

If your loader uses a different run task, use its client run equivalent (for example, a loader-specific `runClient` replacement).

## Runtime output locations

When running from Gradle, runtime artifacts are typically written under the `run/` directory:

- **Logs:** `run/logs/latest.log` (and related files in `run/logs/`)
- **DB files:** any runtime-created database file (for example `*.db`) will appear in `run/` unless configured otherwise in code.

## Known-good test profile versions

- **Minecraft:** `1.21.1`
- **Fabric Loader:** `0.16.9`
- **Fabric API:** `0.116.7+1.21.1`
- **Cobblemon GTS Tracker:** `0.1.0`
- **Client commands:** `/gts ...` and `/gtstracker ...` are both registered and point to the same handlers.

> Verification note: in this headless CI/container environment, client bootstrap reaches mod initialization (including `Cobblemon GTS Tracker initialized.`) but crashes before interactive command entry because no GLFW display is available.
