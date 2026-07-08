# ServerFix Tweaks (1.21.1, NeoForge)

Various configurable fixes for modded Minecraft 1.21.1 — targeting issues encountered with
Sable, Create, Create: Aeronautics, and other mods on a private server.

Every fix ships with its own config toggle in `config/serverfixtweaks-common.toml`, so
individual fixes can be enabled or disabled without removing the mod.

## Fixes

| Fix | Config key | Default | Branch |
|-----|------------|---------|--------|
| Beehive tree decorator crash — vanilla `BeehiveDecorator.place()` reads `logs.get(0)` without an empty check, so modded trees that record no log blocks crash worldgen with `IndexOutOfBoundsException` | `fixes.fixBeehiveDecoratorCrash` | `true` | `fix/beehive-decorator-crash` |

## Development

- Java 21, NeoForge 21.1.x, built with [ModDevGradle](https://github.com/neoforged/ModDevGradle).
- Each fix is developed on its own `fix/<name>` branch.
- Build with `./gradlew build`; the jar lands in `build/libs/`.

## License

MIT — see [LICENSE](LICENSE).
