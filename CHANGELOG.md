# Changelog

## Unreleased

### Added
- Added runtime compatibility reporting (Minecraft/Fabric/Fabric API/Cobblemon/Java) to startup logs and `/gtstracker status`.
- Added GUI open failure handling so command users get feedback and stack traces are persisted to `latest.log`.

### Fixed
- Removed the ModMenu `library` badge from `fabric.mod.json` so this mod is represented correctly in modpacks.
- Reworked release artifact validation so `build/libs/gtstracker-<version>.jar` must come from `remapJar` and build fails when the remapped output is missing/invalid (instead of copying the dev jar).
