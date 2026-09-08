# UOP Recording Tools

A production-oriented Paper plugin for Minecraft 1.21.11, designed to reproduce the externally observable behavior of UltraOP Recording Tools for creator recording workflows and server administration.

## Target
- Minecraft: 1.21.11
- Platform: Paper
- Java: 21+
- Plugin ID: `uop-recording-tools`
- Root command: `/uop`

## Features
Armor/loadouts, checkpoints, global/outgoing/incoming/pair damage scaling, enchant/disenchant, normal/full freeze, inventory editing/backups/history/locking, custom name tags, player identity protection, persistent permissions, reset/reload/help/version, selectors, multiple targets and TAB completion.

## Build

```bash
./gradlew build
```

The plugin jar is produced in `build/libs/`.

## Commands

See the command reference in the original UltraOP Recording Tools repository for the full syntax. This Paper implementation keeps the same public command structure and translates Fabric-specific internals to Paper/Bukkit APIs.

## License

Proprietary — UltraOP.
