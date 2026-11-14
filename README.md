<p align="center"><img src="docs/img/logo.png" width="160" alt="Skill Tree Ascension logo"></p>

# Skill Tree Ascension

An addon for [Passive Skill Tree](https://www.curseforge.com/minecraft/mc-mods/passive-skill-tree) (Forge 1.20.1) that gives it
a new skill screen, extra point types, repeatable skills with levels, trees that only some players get, and over 50 new
skill bonuses. Everything is set up with datapacks and configs, so pack makers decide how it all works.

- **Requires:** Minecraft 1.20.1, Forge 47+, Passive Skill Tree 0.7.6+. Install on client **and** server.
- **Optional:** Iron's Spells 'n Spellbooks, Apotheosis, Origins (Forge).
- **License:** GPL-3.0-or-later, like Passive Skill Tree.

| Page | What's there |
|---|---|
| [Datapacks and configs](docs/datapacks.md) | Point types, repeatable nodes, rank tiers, tree access (origins), stacks, configs, example datapack. |
| [Skill bonuses](docs/bonuses.md) | What every new bonus does, exactly, with its fields and defaults. |
| [Source code and building](docs/building.md) | How the code is organized and how to build it. |

## What it adds

### A new skill tree screen
- Drawn by the mod itself, so it works with any resource pack (and looks great with skill tree texture packs).
- A **side rail** with every class tree and a **radial hub** that replaces the tree selection screen.
- A header with your level, XP to the next point and a counter for every point type. Click "+1 point ready" to buy a point (Shift buys 10).
- Node tooltips with type, state, cost and **how many skills you still need by the shortest path**, plus a summary of all your bonuses.
- Tree order, hidden trees and hub colors are client config options.

### Your own point types
Add Talent, Mastery, Prestige points or anything else next to PST's skill points: name, color, which trees or tagged
skills they pay for, and how they are earned (experience levels, PST points earned, advancements), with caps and refunds.
Admins and quests use `/skilltree points add|set <player> <amount> [type]`.

### Repeatable skills
Any skill can level up, as many times as you want or up to a cap, with its own cost per level, bonus per level, free
levels and **milestones** that run commands at chosen levels. Ranks (Bronze to Diamond by default) show the level.
Set it with a datapack or just type tags like `Repeatable`, `MaxLevel20` or `Points=mypack:talent` in PST's editor.

### Trees only some players get
Lock a tree behind an **origin** or **power** (Origins for Forge), an advancement, a learned skill, a scoreboard tag or a
permission level. Locked trees are hidden for that player and cannot be learned, and they appear by themselves once the
player qualifies.

### Over 50 new skill bonuses
Combat (Fury, Kill Streak, Execute, War Cry, Hunter's Mark...), utility (Last Page, Learning Blade, Memento Mori...),
enchanting, pets, Iron's Spells (Arcane Echo, Mana Flow, Overchannel, School Chain, Stormcaller, Rune Scribe) and
Apotheosis gems. Every number is configurable. See [bonuses.md](docs/bonuses.md).

### Fixes for Passive Skill Tree
Learning and point buying run on the server thread, skill syncs are merged (no screen rebuild storms), effect bonuses no
longer stack to the max in one hit, and the screen no longer breaks when it opens before your skill data arrives.

## Credits

Built on [Passive Skill Tree](https://github.com/Daripher/Passive-Skill-Tree) by Daripher. This addon is not made by
the PST author: report its bugs here, not to PST. Skill tree textures in screenshots may come from third-party resource
packs, which are not part of this mod.
