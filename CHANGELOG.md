# Changelog

## 1.2.1
- Fixed: attribute bonuses with a condition (for example "while holding an axe" or "while your hunger bar is full") did nothing when they were part of a repeatable skill's milestone.
- Fixed: conditional attribute bonuses on a repeatable skill now scale with its level instead of staying at the level 1 value.
- These bonuses are removed again when the skill is reset or the milestone is no longer reached.

## 1.2.0
- New: skill requirements. A node rule can ask for an item, an offering (consumed on learn), kills, items used or blocks mined, a vanilla statistic, a stack, another skill's level, an advancement, an origin or a power (`requires` in `data/<ns>/skillascension/nodes`). Requirements check what the player has or has done, never crafting events, so they work with autocrafting.
- New: exclusive choices. Skills in the same `exclusive` group lock each other: learning one closes the others.
- The tooltip shows each requirement with ✔ / ✖ and the other side of a choice; nodes with missing requirements cannot be learned.

## 1.1.0
- New: trees only some players get. Lock a tree behind an origin or power (Origins for Forge), an advancement, a learned skill, a scoreboard tag or a permission level (`data/<ns>/skillascension/tree_access`).
- Fixed: Enchanting Discount, Bonus Enchantment and Enchantment Amplification did nothing. They now work on the vanilla enchanting table and on Apotheosis' table.
- Fixed: Fury, War Cry, Hunter's Mark and Anvil timers could last much longer or shorter than set.
- Fixed: missing messages for Last Page, Learning Blade and the bonus summary.
- Docs: exact description of every bonus (docs/bonuses.md).

## 1.0.0
- First release.
