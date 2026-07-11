# ServerFix Tweaks (1.21.1, NeoForge)

Various configurable fixes for modded Minecraft 1.21.1 — targeting issues encountered with
Sable, Create, Create: Aeronautics, and other mods on a private server.

Every fix ships with its own config toggle in `config/serverfixtweaks-common.toml`, so
individual fixes can be enabled or disabled without removing the mod.

## Fixes

| Fix | Config key | Default | Branch |
|-----|------------|---------|--------|
| Beehive tree decorator crash — vanilla `BeehiveDecorator.place()` reads `logs.get(0)` without an empty check, so modded trees that record no log blocks crash worldgen with `IndexOutOfBoundsException` | `fixes.fixBeehiveDecoratorCrash` | `true` | `fix/beehive-decorator-crash` |
| Sable empty-contraption crash ([ryanhcode/sable#1315](https://github.com/ryanhcode/sable/issues/1315)) — Sable builds physics properties for Create contraptions without null-checking the bounding box or centre of mass, so a contraption with no solid blocks `NullPointerException`-crash-loops the server on its first tick. Guards both spots so Sable's own empty-contraption path handles them. Written against Sable 2.x (verified on 2.0.3) + Create 6.0.10; only applies when both are installed | `fixes.fixSableEmptyContraptionCrash` | `true` | `fix/sable-empty-contraption-crash` |
| Create collision null-axis crash ([Create#10218](https://github.com/Creators-of-Create/Create/issues/10218), dup [#10479](https://github.com/Creators-of-Create/Create/issues/10479)) — `ContinuousOBBCollider.collideMany` dereferences a null separation axis when an entity's collision box centre exactly coincides with a contraption collider's centre (item drops from drills on pulleys, bearing tree farms, ...), crash-looping the world. Degenerate colliders now contribute a zero response instead. Verified on Create 6.0.10; only applies when Create is installed | `fixes.fixCreateCollisionNullAxisCrash` | `true` | `fix/create-collision-null-axis` |
| Sable voxel-cache race crash ([ryanhcode/sable#1292](https://github.com/ryanhcode/sable/issues/1292), older reports [#788](https://github.com/ryanhcode/sable/issues/788), [#1223](https://github.com/ryanhcode/sable/issues/1223)) — Sable memoizes per-`BlockState` solidity in unsynchronized `Int2BooleanOpenHashMap`s shared by all threads; concurrent block updates silently corrupt the maps until a rehash throws `ArrayIndexOutOfBoundsException: Index -1`, crash-looping the world on every subsequent block update. Cache accesses are now synchronized. Verified on Sable 2.0.3; only applies when Sable is installed | `fixes.fixSableVoxelCacheRaceCrash` | `true` | `fix/sable-voxel-cache-race` |

## Development

- Java 21, NeoForge 21.1.x, built with [ModDevGradle](https://github.com/neoforged/ModDevGradle).
- Each fix is developed on its own `fix/<name>` branch.
- Build with `./gradlew build`; the jar lands in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).
