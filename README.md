# UOP Recording Tools

Production-oriented Paper plugin for Minecraft 1.21.11, designed for creator recording workflows and behavioral parity with UltraOP Recording Tools Fabric.

## Target
- Minecraft 1.21.11
- Paper
- Java 21+
- Plugin ID: `uop-recording-tools`
- Root command: `/uop`

## Command Reference

### Armor
- `/uop armor set <targets> <material> <plain|enchanted 1-10|max> [true|false] [weapons...]`
- `/uop armor save <name> <player>`
- `/uop armor list`
- `/uop armor delete <name>`
- `/uop armor give <targets> saved <name>`
- `/uop armor give <targets> direct <material> <plain|enchanted 1-10|max> [true|false] [weapons...]`
- `/uop armor weapon set <targets> <plain|enchanted 1-10|max> <weapons...>`

Materials: leather, chainmail, iron, gold, diamond, netherite.
Weapons/tools: sword, mace, axe, bow, crossbow, trident, pickaxe, shovel, hoe.

### Checkpoints
- `/uop checkpoint save <name> [targets]`
- `/uop checkpoint tp <name>`
- `/uop checkpoint list`
- `/uop checkpoint delete <name>`
- `/uop checkpoint clear all`

### Damage
- `/uop damage global <0-10>`
- `/uop damage outgoing <player> <0-10>`
- `/uop damage incoming <player> <0-10>`
- `/uop damage set <attacker> <victim> <0-10>`
- `/uop damage reset <global|outgoing|incoming|pair|all> [player] [victim]`

Priority is exact pair > outgoing > incoming > global. A zero multiplier changes damage to zero without cancelling the underlying damage event, preserving mechanics such as Wind Charge propulsion.

### Enchant / Disenchant
- `/uop enchant <all|armor|equipment|mainhand|offhand|inventory> <targets> <enchantment> [level]`
- `/uop disenchant <all|armor|equipment|hand|inventory> <targets> <enchantment>`
- Armor and disenchant hand scopes accept a slot selector where applicable.
- Enchantment levels are validated to 1-255 and cannot exceed the enchantment's native maximum.
- `/uop enchant hand` is intentionally unsupported; use `mainhand` or `offhand`.

### Freeze
- `/uop freeze [normal|full] <targets>`
- `/uop unfreeze <targets>`

Normal freeze blocks movement, world interaction, item drop/pickup while permitting intended internal inventory rearrangement and armor changes. Full freeze additionally blocks rotation, inventory mutation, hand changes and relevant actions. ESC remains the normal Minecraft menu key because the plugin does not intercept it. OP and `uop.freeze.exempt` players are exempt.

### Inventory
- `/uop inv view <player>`
- `/uop inv set <player> <slot 0-40> <item> [amount 1-99]`
- `/uop inv remove <player> <slot 0-40>`
- `/uop inv give <player> <item> [amount 1-99]`
- `/uop inv take <player> <item> [amount 1-99]`
- `/uop inv swap <player> <slot> <player> <slot>`
- `/uop inv clear <player>`
- `/uop inv backup <player> <name>`
- `/uop inv restore <player> <name>`
- `/uop inv backups <player>`
- `/uop inv backup-delete <player> <name>`
- `/uop inv record start <player>`
- `/uop inv record stop <player>`
- `/uop inv back <player> <seconds|Nm|Nh>`
- `/uop inv lock <player>` / `unlock <player>`

History is sampled on a configurable tick interval and retained as a bounded rolling list. The default sampling interval is 100 ticks (5 seconds).

### Names and Tags
- `/uop name hide <targets>` / `show <targets>`
- `/uop name tag create <id> <color|#RRGGBB> <bold> <italic> <text>`
- `/uop name tag edit <id> <color|#RRGGBB> <bold> <italic> <text>`
- `/uop name tag list`
- `/uop name tag give <targets> <id>`
- `/uop name tag remove <targets>`
- `/uop name tag delete <id>`

Tag text is limited to 32 characters. Tags persist by player UUID and are reapplied after reconnect.

### Protection
- `/uop protect add <targets>` (default: both UUID + IP)
- `/uop protect uuid <targets>`
- `/uop protect ip <targets>`
- `/uop protect both <targets>`
- `/uop protect remove <targets>`
- `/uop protect list`
- `/uop protect reset`

Protection is checked during pre-login and rejects mismatched identities without replacing a legitimate active player. Behind proxies, configure forwarding/network identity carefully because the observed IP may be the proxy address.

### Permissions / Administration
- `/uop permission grant <player> <nodes...>`
- `/uop permission revoke <player> <nodes...>`
- `/uop permission list <player>`
- `/uop permission check <player> <node>`
- `/uop permission reset <player> [node]`
- `/uop permission reset all`
- `/uop reload`
- `/uop reset all`
- `/uop help`
- `/uop version`

Permission administration is restricted to actual OPs or the server console. Persistent wildcard `*` and hierarchical `node.*` permissions are supported.

## Persistence

Persistent data is stored in `plugins/UOP-Recording-Tools/data/data.yml`, including damage rules, freezes, checkpoints, loadouts, names/tags, protected identities, permissions, backups and history.

## Build

```bash
gradle clean build
```

The plugin JAR is produced in `build/libs/`.

## License

Proprietary — UltraOP.
