# Skill Tree Ascension

**An addon for Passive Skill Tree that gives it a new skill screen, extra point types, repeatable skills with levels, and over 50 new skill bonuses. Everything is set up with datapacks and configs, so pack makers decide how it all works.**

Requires [Passive Skill Tree](https://www.curseforge.com/minecraft/mc-mods/passive-skill-tree) 0.7.6 or newer (Forge 1.20.1). Iron's Spells 'n Spellbooks, Apotheosis and Origins are optional.

---

## A new skill tree screen

- A cleaner layout drawn by the mod itself, so it works with any resource pack (and looks great with skill tree texture packs).
- A **side rail** with every class tree: jump between trees in one click.
- A **radial hub** that replaces the tree selection screen, showing your progress in each tree.
- A header with your level, XP to the next point, and a counter for every point type. Click the "+1 point ready" chip to buy a point (Shift buys 10).
- Clear node tooltips: node type (minor, notable, keystone, gateway), state (learned, can learn, not reachable yet), cost, and **how many skills you still need by the shortest path**.
- A summary of all your bonuses merged together.
- Tree order, hidden trees and hub colors are set in the client config.

## Your own point types

Add any number of new currencies next to PST's skill points, for example Talent, Mastery or Prestige points.

- Each type has its own name, color, cost, header counter and button.
- Choose which trees or which tagged skills cost each type.
- Earn them from experience levels, from PST skill points earned, or from advancements. Mix as many sources as you like, with caps.
- Optional cap on how many a player can hold, and refund on tree reset.
- Admin commands added to PST's own `/skilltree points`: `add|set <player> <amount> [type]` and `get <player> [type]` (no type = PST's skill points, as before).

## Repeatable skills

Turn any skill into a skill with levels.

- Level it up as many times as you want, or up to a cap.
- Set the cost per level, how much of the bonus each level adds, and free levels every N levels.
- **Milestones:** run commands and show a message when a skill reaches a given level (give items, effects, unlock things, anything a command can do).
- Ranks shown by level (Bronze, Silver, Gold, Platinum, Diamond by default), fully replaceable.
- In game: click the skill, use - / + (Shift for 5) or Max, then Confirm.
- Works for attribute bonuses too.

Set it up per skill, per tree or per tag with a datapack, or just type tags like `Repeatable`, `MaxLevel20`, `LevelCost2` or `Points=mypack:talent` on the skill in PST's editor.

## Trees only some players get

Lock a tree behind an **origin** or **power** (Origins for Forge, optional), an advancement, a learned skill, a scoreboard tag or a permission level. Locked trees are hidden for that player and cannot be learned, and they appear by themselves as soon as the player qualifies.

## Over 50 new skill bonuses

**Combat:** Fury, Kill Streak, Execute, Counter (after a shield block), War Cry, Hunter's Mark, Anvil (armor shred), Overheal, Harpoon, Bond (fight next to a friend), High Tide, Shell, Shadowstep, Shadowveil.

**Utility and experience:** Last Page (cheat death for levels), Level Surge, Experience Interest, Learning Blade (kills can raise your weapon's enchantments), Living Forge (armor repairs as you get hit), Feast and Shared Potion (share food and potions with nearby players), Memento Mori (keep XP on death), Night Vigil (sleep with monsters near), Witch Doctor, Treasure Seeker, Catch XP, Experience Aegis, Experience Siphon.

**Enchanting** (vanilla table and Apotheosis): cheaper enchanting, extra enchantments from the table, higher enchantment levels.

**Pets** (any tamed animal): Pack Leader, Pack Hide, Feral Bond, Blood Pact, Den, Prey, Pincer, Frenzy, Guardian, Beast Shield, Vengeance, Savage, Spiked Collar, Howl, Undying Bond.

**Iron's Spells** (optional): Arcane Echo, Mana Flow, Overchannel, School Chain, Stormcaller, Rune Scribe.

**Apotheosis** (optional): Gem Power.

Every number in every bonus can be changed in the skill file, and most bonuses accept PST's item conditions and multipliers.

## New value providers for any PST bonus

- **Stacks:** lifetime counters that grow as you play (melee hits, kills, bosses, spells per school, fish, pets, crafting, brewing, cooking and more). Combine them into named stacks in `config/skillascension/stacks.json` and make bonuses grow with them. Built so they cannot be farmed by AFK mob farms.
- Experience level, gems socketed, curses on your gear, total spell power.

## Fixes for Passive Skill Tree

- Skill learning and point buying run on the server thread (no more rare desyncs).
- Skill syncs are merged, so the screen does not rebuild many times in a row.
- Effect bonuses no longer stack to the max in a single hit.
- No crash when the screen opens before your skill data arrives.
- Optional extra class picks when playing alone, and a way to hide internal limitation tags from tooltips.

## For pack makers

Everything lives in `data/<namespace>/skillascension/` (point types, node rules, rank tiers) and reloads with `/reload`. Full documentation, the exact behavior of every bonus and an example datapack are on the source page (GitHub). The mod ships no skill trees of its own: build yours with PST's editor.

---

Skill Tree Ascension is not made by the author of Passive Skill Tree. Please report bugs with this addon here, not to PST. Licensed GPL-3.0, like PST.
