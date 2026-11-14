# Source code and building

The code is in [`src/main/java/dev/skillascension`](../src/main/java/dev/skillascension):

| Package | What it does |
|---|---|
| `client`, `client/pstui` | The new skill tree screen (side rail, radial hub, header, tooltips, bonus summary). Everything is drawn in code, no textures. |
| `data` | Datapack loading: point types, node rules, rank tiers, tree access; synced to clients. |
| `points` | Point wallets, point sources, the `/skilltree points` command, tree access updates. |
| `pst` | Repeatable skills, server-side learning, all the event-based bonuses. |
| `pst/bonus`, `pst/function` | The new PST bonus and value provider types. |
| `pets` | Pet bonuses. |
| `stacks` | Lifetime counters and named stacks. |
| `compat` | Optional Iron's Spells, Apotheosis and Origins/Apoli support (only loaded when those mods are present). |
| `mixin` | Changes to PST and Minecraft. |
| `net` | Network messages. |

## Building

1. Install a Java 17 JDK.
2. Put the jars listed in [`libs/README.md`](../libs/README.md) in `libs/`.
3. Run `./gradlew build` (Windows: `gradlew.bat build`). The mod is in `build/libs/`.


Notes:

- Mixins use Minecraft's SRG names with `remap = false`, so they apply in a normal game; in a ForgeGradle dev run
  (official names) the injections into Minecraft classes do not apply. Test with the built jar.
- Iron's Spells, Apotheosis and Origins code is isolated in `compat` and only touched when those mods are loaded.

## License

GPL-3.0-or-later, like Passive Skill Tree. See [LICENSE](../LICENSE).
