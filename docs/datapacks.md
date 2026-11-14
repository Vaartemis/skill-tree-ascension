# Datapacks and configs

Everything here is set up with datapacks (`data/<namespace>/skillascension/...`, reload with `/reload`) and two config
files (`config/skillascension-common.toml`, `config/skillascension-client.toml`). The bonuses themselves are in
[bonuses.md](bonuses.md).

## Point types

`data/<ns>/skillascension/point_types/<name>.json`. The file name is the id (`<ns>:<name>`).
PST's own skill points are the built-in type `skillascension:skill_points`.

| Field | Default | Meaning |
|---|---|---|
| `name` | from the file name | Text or text component shown in the UI. Lang key `skillascension.point_type.<ns>.<name>` also works. |
| `color` | `#7FB2FF` | Color of the chip, counter and costs. |
| `trees` | none | PST tree ids whose skills cost this point type. |
| `skill_tag` | none | Skills with this PST tag cost this point type, in any tree. |
| `cost` | 1 | Points to learn one skill. |
| `open_tree` | the only tree in `trees` | Tree opened by this type's button in the header and hub. |
| `icon_tree` | none | Tree whose icon is used for the button. |
| `show_in_header` | true | Show the counter at the top of the skill screen. |
| `sort` | 100 | Order in the header (native points are 0). |
| `refund_on_reset` | true | Give the points back when the tree is reset. |
| `max` | 0 (no cap) | Most points a player can hold. |
| `sources` | none | How players earn it (below). |

Sources (each is counted separately and only pays the difference, so changing the numbers later is safe):

- `{"type":"xp_level","every":10,"amount":1,"max":0}`: `amount` for every `every` experience levels reached (highest level ever, so spending levels does not take points away).
- `{"type":"skill_points_earned","every":5,"amount":1}`: `amount` for every `every` PST skill points earned.
- `{"type":"advancement","advancement":"minecraft:story/mine_diamond","amount":1}`: once, when the advancement is earned.

`max` on a source caps what that source can ever give (0 = no cap).



## Node rules (repeatable skills, costs, levels)

`data/<ns>/skillascension/nodes/<any>.json`, either one rule or `{"rules":[...]}`. A rule picks skills with
`skills` (skill ids), `trees` (tree ids) or `tag` (PST skill tag). More specific rules win:
point type claims < skill tag shortcuts < tree rules < tag rules < skill rules.

| Field | Meaning |
|---|---|
| `points` | Point type id the skill costs. |
| `cost` | Points to learn it. |
| `repeatable` | The skill can be leveled up after learning. Every level adds its bonuses again. |
| `max_level` | Level cap (0 = no cap). |
| `level_cost` | Points per extra level (default = `cost`). |
| `free_level_every` | Only for uncapped skills: one free level every N levels (0 = off, the default). |
| `bonus_per_level` | How much of the skill's bonuses each level adds (1.0 = a full copy, 0.5 = half). Attribute bonuses are scaled too. |
| `milestones` | `[{"level":10,"commands":["..."],"message":"..."}]`. Commands run as the player (permission 4, output hidden) the first time the level is reached. `command` (one string) also works. |

Shortcuts you can type as PST skill tags in the PST editor, no datapack needed:
`Repeatable`, `MaxLevel20`, `LevelCost2`, `Cost3`, `Points=mypack:talent`, `IconScale150`.

Leveling in game: click a learned repeatable skill, use - / + (Shift = 5) or Max, then Confirm.

## Commands

PST's own `/skilltree points` gets an optional type: `/skilltree points add|set <player> <amount> [type]`, `/skilltree points get <player> [type]`. Without a type, `add` and `set` work on PST's skill points exactly as before.

## Rank tiers

`data/<ns>/skillascension/rank_tiers/<any>.json`: `{"replace": true, "tiers":[{"level":10,"name":"Silver","color":"#D8DCE3"}]}`.
Names shown on repeatable skills by level. The built-in file is `skillascension:default` (Bronze 1, Silver 10, Gold 20,
Platinum 30, Diamond 50). Set `"replace": false` to add tiers instead of replacing them. A name with a dot and no
spaces is used as a lang key.

## Tree access (trees only some players get)

`data/<ns>/skillascension/tree_access/<any>.json`. A locked tree is hidden from that player (side rail, hub and
PST's tree list) and the server refuses to let them learn its skills. It updates by itself every 2 seconds, so a tree
appears as soon as the player picks the right origin, earns the advancement, and so on.

```json
{
  "trees": ["mypack:dragon_knight"],
  "mode": "any",
  "requires": [
    { "type": "origin", "origin": "origins:elytrian" },
    { "type": "power", "power": "mypack:dragon_blood" },
    { "type": "advancement", "advancement": "minecraft:end/kill_dragon" },
    { "type": "tag", "tag": "dragonborn" }
  ]
}
```

- `trees`: the tree ids this file locks.
- `mode`: `any` (default, one condition is enough) or `all` (every condition is needed).
- A tree named in several files needs to pass every file.
- A file with no `requires` closes the tree to everyone (handy to switch a tree off).

Conditions (each can also take `"inverted": true` to mean "does NOT have"):

| `type` | Fields | Passes when the player... |
|---|---|---|
| `origin` | `origin`, optional `layer` | has that origin (Origins for Forge; in that layer, or any layer). |
| `power` | `power`, optional `active` [false] | has that Apoli power, from an origin or any other source. With `"active": true`, only while the power is active (for example an `origins:toggle` power switched on with its key), so players can show and hide the tree themselves. |
| `advancement` | `advancement` | has completed the advancement. |
| `skill` | `skill` | has learned that skill (any tree), for trees that unlock from another tree. |
| `tag` | `tag` | has that scoreboard tag (`/tag @s add dragonborn`), so any mod, quest or command block can unlock a tree. |
| `permission` | `level` [2] | has that permission level (for admin or test trees). |

Example: a tree only players of one origin can open, by switching on a toggle power of that origin:

```json
{ "trees": ["mypack:dragon"], "requires": [{ "type": "power", "power": "mypack:dragon_sight", "active": true }] }
```

Everyone else never sees the tree (players without the power can't turn it on).

Origins and Apoli are optional: without them, `origin` and `power` conditions are simply false.

## Stacks

`config/skillascension/stacks.json` (written with defaults on first start). Each named stack is a weighted sum of
counters the mod keeps per player for life, for example `"fury": {"c.hits_melee": 0.02, "c.damage_melee": 0.002}`.
Use it in any PST bonus as a value provider: `{"type":"skillascension:stacks","stack":"fury"}`.
Counters only grow from real fights (threats that fight back, no farms in one chunk), so they cannot be AFK farmed easily.

Counters: `hits_dealt, damage_dealt, hits_melee, damage_melee, hits_ranged, damage_ranged, arrow_damage, arrows_shot,
hits_spell, damage_spell, damage_spell:<school>, spells_cast, spells_cast:<school>, mana_spent, kills, kills_melee,
kills_ranged, kills_spell, kills_boss, kills_wet, hits_taken, damage_taken, blocks, damage_blocked, fish_caught,
treasure_caught, pet_kills, pet_damage, pet_hits, pet_damage_taken, tames, species_tamed, enchant_levels, anvil_enchants,
anvil_uses, items_crafted, items_smelted, potions_brewed, potions_drunk, debuffs_applied, food_cooked, food_eaten`,
plus raw stacks `trophies` and `essence:<school>` / `essence:*`. Spell counters need Iron's Spells 'n Spellbooks.

## Configs

Common: `singleplayerLimitBonus` and `singleplayerLimitTags` (extra class picks when playing alone),
`hiddenLimitPattern` (regex of limitation tags to hide from tooltips), `effectStackCooldownTicks`, `levelUpMessages`.

Client: `enabled` (new skill screen), `hub` (radial tree picker), `treeOrder`, `hiddenFromRail`, `treeColors`.

## Example datapack

[`example_datapack/`](../example_datapack) adds a Talent point type (1 every 10 levels, 2 for entering the Nether, 1 every 5 skill points up
to 10), makes every skill tagged `Talent` repeatable, and gives one skill a level cap, half bonus per level and two
milestones, and locks a tree `example:dragon` (if you make one) to Elytrian players or players with the `dragonborn` tag. It contains no skill trees: make your own with PST's editor.

