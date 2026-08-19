# ServerFix Tweaks (1.21.1, NeoForge)

Various configurable fixes for modded Minecraft 1.21.1 — targeting issues encountered with
Sable, Create, Create: Aeronautics, and other mods on a private server.

Every fix ships with its own config toggle in `config/serverfixtweaks-common.toml`, so
individual fixes can be enabled or disabled without removing the mod.

## Fixes

| Fix | Config key | Default | Branch |
|-----|------------|---------|--------|
| Beehive tree decorator crash — vanilla `BeehiveDecorator.place()` reads `logs.get(0)` without an empty check, so modded trees that record no log blocks crash worldgen with `IndexOutOfBoundsException` | `fixes.fixBeehiveDecoratorCrash` | `true` | `fix/beehive-decorator-crash` |
| Create collision null-axis crash ([Create#10218](https://github.com/Creators-of-Create/Create/issues/10218), dup [#10479](https://github.com/Creators-of-Create/Create/issues/10479)) — `ContinuousOBBCollider.collideMany` dereferences a null separation axis when an entity's collision box centre exactly coincides with a contraption collider's centre (item drops from drills on pulleys, bearing tree farms, ...), crash-looping the world. Degenerate colliders now contribute a zero response instead. Verified on Create 6.0.10; only applies when Create is installed | `fixes.fixCreateCollisionNullAxisCrash` | `true` | `fix/create-collision-null-axis` |
| Sable voxel-cache race crash ([ryanhcode/sable#1292](https://github.com/ryanhcode/sable/issues/1292), older reports [#788](https://github.com/ryanhcode/sable/issues/788), [#1223](https://github.com/ryanhcode/sable/issues/1223)) — Sable memoizes per-`BlockState` solidity in plain, unsynchronized fastutil maps held in `static` fields of `VoxelNeighborhoodState`, so every thread that reaches `isSolid`/`isFullBlock` shares them — the server thread and, in single player, the client thread handling the same block change. Interleaved writes silently corrupt the maps until a later rehash throws `ArrayIndexOutOfBoundsException: Index -1`, crash-looping the world on every subsequent block update. Whole memoizer calls are now serialized on the memoizer instance. Verified on Sable 2.0.5; only applies when Sable is installed | `fixes.fixSableVoxelCacheRaceCrash` | `true` | `fix/sable-voxel-cache-race`, `fix/sable-voxel-cache-reentrancy`, `fix/sable-voxel-cache-sync` |

Sable fixes only apply on Sable `[2.0.4,2.1.0)`. Outside that range they are skipped with a warning in
the log rather than risking a startup crash on a Sable version whose internals have moved.

## Retired fixes

| Fix | Config key | Retired because |
|-----|------------|-----------------|
| Sable empty-contraption crash ([ryanhcode/sable#1315](https://github.com/ryanhcode/sable/issues/1315)) — Sable built physics properties for Create contraptions without null-checking the bounding box or centre of mass, so a contraption with no solid blocks `NullPointerException`-crash-looped the server on its first tick | `fixes.fixSableEmptyContraptionCrash` | Fixed upstream in Sable 2.0.4 (null local bounds) and 2.0.5 (`MassData.isInvalid()`). Upstream's guard is wider than this mod's was: `isInvalid()` is `getMass() <= 0.0 \|\| getCenterOfMass() == null`, so it also catches a contraption whose blocks are all solid but all massless. `MassTracker.build` only returns a null centre of mass when it counted *zero* solid blocks; with solid-but-massless blocks it divides by a zero total mass instead, producing a `NaN` centre of mass and an infinite (`1.0/0.0`) inverse mass that this mod's null-check let straight through. The mixins are gone as of Sable 2.0.5 support; the config key is kept as a no-op so existing config files stay valid |

## Development

- Java 21, NeoForge 21.1.x, built with [ModDevGradle](https://github.com/neoforged/ModDevGradle).
- Each fix is developed on its own `fix/<name>` branch.
- Build with `./gradlew build`; the jar lands in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).
