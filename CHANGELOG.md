# Changelog

## Unreleased

### Added
- Added runtime compatibility reporting (Minecraft/Fabric/Fabric API/Cobblemon/Java) to startup logs and `/gtstracker status`.
- Added GUI open failure handling so command users get feedback and stack traces are persisted to `latest.log`.

### Fixed
- Removed the ModMenu `library` badge from `fabric.mod.json` so this mod is represented correctly in modpacks.
- Added a release-artifact safeguard that replaces an unexpectedly empty remap output with a usable fallback jar at `build/libs/gtstracker-<version>.jar`.
