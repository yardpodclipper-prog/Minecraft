# Release Notes

## Compatibility (Pinned/Tested)

Tested against **CobbleGalaxy 1.7.3 stack** with the following exact versions:

| Component | Min Tested | Max Tested |
| --- | --- | --- |
| Minecraft | 1.21.1 | 1.21.1 |
| Fabric Loader | 0.18.4 | 0.18.4 |
| Fabric API | 0.116.8+1.21.1 | 0.116.8+1.21.1 |
| Cobblemon | 1.7.3+1.21.1 | 1.7.3+1.21.1 |

No compatibility outside of the pinned range above is currently guaranteed.

## Baseline Regression Build

- **Baseline tag:** `baseline-remap-ci-fixes`
- **Build artifact source:** `build/libs`
- **Validation target profile:** Minecraft `1.21.1` + Fabric Loader `0.18.4` + Fabric API `0.116.8+1.21.1` + Cobblemon `1.7.3+1.21.1`

### Validation checklist

- [ ] Clean CI build completed from scratch.
- [ ] Release artifact uploaded from `build/libs`.
- [ ] Tested in the real CobbleGalaxy modpack profile using the pinned versions above.
- [ ] Verified no startup exceptions in `latest.log`.
- [ ] Verified core mod initialization and command paths (`/gtstracker status`, `/gtstracker gui`).

