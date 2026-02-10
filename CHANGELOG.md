# Changelog

## Unreleased

### Fixed
- Added a `liveJar` packaging step to guarantee `build/libs/gtstracker-0.1.0.jar` contains mod classes/resources for deployment testing.
- Added safer GUI open/render/refresh error handling that logs failures to `latest.log` instead of failing silently.

### Changed
- Startup logging now records Minecraft/Fabric Loader compatibility context and Cobblemon loaded state for production diagnostics.
- Cleaned up Fabric metadata by removing the Mod Menu `library` badge.
