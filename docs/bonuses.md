# Skill bonuses

Every bonus is used like any PST bonus inside a skill file:

```json
{ "type": "skillascension:fury", "amount": 0.02, "max_stacks": 15, "decay": 4 }
```

How to read this page:

- **`amount`** is a fraction: `0.1` means 10%. The few bonuses that use a flat number say so.
- The other fields are optional. Their defaults are in brackets.
- **Several skills with the same bonus add their `amount` together.** For the other fields, the best value counts (usually the highest; for cooldowns and costs, the lowest).
- Bonuses work with PST's `player_multiplier` and, where it makes sense, `item_condition`.
- Repeatable skills: every extra level adds the bonus again (times `bonus_per_level`).

---

## Iron's Spells 'n Spellbooks (optional)

These need Iron's Spells installed. "Spell damage" means damage from an Iron's spell.

**Arcane Echo** (`arcane_echo`)
When you cast a spell, `amount` chance (capped at 35%) that it costs no mana. If it procs, the spell's cooldown is also removed as soon as it starts (within 10 seconds of the cast). Shows "Arcane Echo!" with a sound and particles.

**Mana Flow** (`mana_flow`, `mana` [100], `duration` [6 s])
Every `mana` mana you spend on spells (minimum 10) gives one Mana Flow stack, up to 10. Each stack gives +`amount` spell damage. Every new stack refreshes the timer; when `duration` seconds pass without a new stack, all stacks are lost. Mana spent below the threshold is kept toward the next stack.

**Overchannel** (`overchannel`, `threshold` [25%])
When you cast a spell while your mana is under `threshold`% of your max mana, your spell damage is +`amount` for the next 3 seconds.

**School Chain** (`school_chain`, `max_stacks` [5], `window` [6 s])
Casting a spell from a different school than your previous spell gives one stack, up to `max_stacks`. Each stack gives +`amount` spell damage. Casting the same school again keeps your stacks and refreshes the timer. If you don't cast anything for `window` seconds, the stacks are lost.

**Stormcaller** (`stormcaller`, `damage` [6])
While you are in water, rain or a bubble column, each time one of your spells damages a target there is `amount` chance (capped at 50%) to call lightning on it. The lightning is visual only (no fire, no charged creepers) and deals `damage` extra magic damage. At most once per second.

**Rune Scribe** (`rune_scribe`)
When you cast a spell from a scroll, `amount` chance to get the scroll back. Does nothing in creative mode.

---

## Combat

**Fury** (`fury`, `max_stacks` [10], `decay` [3 s])
Each melee hit gives a stack, up to `max_stacks`. Each stack gives +`amount` melee damage (the hit that adds a stack uses the stacks you had before it). Stacks reset if you go `decay` seconds without a melee hit.

**Kill Streak** (`kill_streak`, `max_stacks` [10], `window` [8 s])
Each kill gives a stack, up to `max_stacks`. Each stack gives +`amount` damage on all your hits. The stacks are lost if you don't kill anything for `window` seconds.

**Execute** (`execute`, `threshold` [25%])
+`amount` damage against targets below `threshold`% of their max health.

**Counter** (`counter`, `window` [3 s])
After you block a hit with a shield, your next melee hit within `window` seconds deals +`amount` damage.

**War Cry** (`war_cry`, `threshold` [50%], `cooldown` [60 s])
When a hit takes you under `threshold`% health (and doesn't kill you), you roar. Enemies within 8 blocks get Weakness II and Slowness II for 6 seconds. You and every player within 8 blocks deal +`amount` damage for 8 seconds. Then it waits `cooldown` seconds before it can trigger again.

**Hunter's Mark** (`hunters_mark`, `duration` [8 s])
Your first projectile hit on a target that isn't marked yet marks it for `duration` seconds and makes it glow. A marked target takes +`amount` damage from everyone (players, mobs, pets).

**Anvil** (`anvil`, `max_stacks` [10], `duration` [5 s])
Each of your melee hits removes `amount` armor points from the target (flat number), stacking up to `max_stacks` times. The shred lasts `duration` seconds after the last hit.

**Harpoon** (`harpoon`, `pull` [0.6])
Thrown tridents (and modded tridents) deal +`amount` damage and pull the target toward you with strength `pull`, reduced by its knockback resistance. Doesn't pull targets that are already within 2 blocks.

**Bond** (`bond`, `radius` [16])
While another player is within `radius` blocks, you deal +`amount` damage and take -`amount` damage (each capped at 25%).

**High Tide** (`high_tide`, `max_stacks` [5], `interval` [10 s])
Every `interval` seconds in water, rain or a bubble column you gain a stack, up to `max_stacks`. Each stack gives +`amount` damage on all your hits. After `interval` seconds out of the water, all stacks are lost.

**Shell** (`shell`, `max` [10])
When you take damage while wet (water, rain, bubble column, or the "wet" effect from Traveloptics), you get `amount` absorption (flat), up to `max` total. At most once per second.

**Overheal** (`overheal`, `max` [10])
When you are healed above your max health, `amount` of the extra healing becomes absorption, up to `max` absorption.

**Shadowstep** (`shadowstep`)
+`amount` movement speed while crouching.

**Shadowveil** (`shadowveil`)
Mobs notice you from `amount` closer (`0.3` = they need to be 30% closer to see you). Never below 10% of the normal range.

---

## Utility and experience

**Last Page** (`last_page`, `cost` [10 levels], `cooldown` [180 s])
When a hit would kill you, you pay `cost` experience levels instead. The hit is canceled, your health is set to `amount` of your max health, and you get Resistance III for 3 seconds. Needs enough levels. Works again after `cooldown` seconds (shown on the cooldown HUD).

**Level Surge** (`level_surge`, `absorption` [8], `duration` [20 s])
When you gain a level: heal `amount` of your max health, and get Absorption for `duration` seconds (about `absorption` extra hearts' worth, in steps of 4). At most once every 3 seconds.

**Experience Interest** (`xp_interest`, `interval` [30 s])
Every `interval` seconds you get `amount` experience points per level you have (flat; `0.5` at level 40 gives 20 points).

**Learning Blade** (`learning_blade`, `over_max` [2])
When you kill something, `amount` chance that one random enchantment on your main-hand weapon goes up by 1. Only enchantments with more than one level count, and they can go up to their max level + `over_max`. Curses never grow.

**Living Forge** (`living_forge`)
Every time you take damage, each armor piece you wear is repaired by `amount` durability (flat, at least 1).

**Feast** (`feast`, `radius` [8])
When you eat, every other player within `radius` blocks heals `amount` × the food's hunger points.

**Shared Potion** (`shared_potion`, `radius` [8])
When you drink a potion, every other player within `radius` blocks gets its effects at `amount` of the duration (instant effects at `amount` strength, max 100%).

**Memento Mori** (`memento_mori`)
When you die (without keepInventory), you keep `amount` of your experience (max 100%). It comes back when you respawn and your dropped experience is reduced by the same share.

**Night Vigil** (`night_vigil`)
You can sleep with monsters nearby. (Any amount above 0 works.)

**Witch Doctor** (`witch_doctor`)
When a Totem of Undying saves you, `amount` chance to get the totem back.

**Treasure Seeker** (`treasure_seeker`)
When you kill an undead mob (not a boss in `#forge:bosses`), `amount` chance its drops are doubled.

**Catch XP** (`catch_xp`)
+`amount` experience points every time you fish something up (flat).

**Experience Aegis** (`xp_aegis`)
`amount` of the damage you take (capped at 60%) is paid with experience instead, at 2 experience points per point of damage. Only when you have enough experience.

**Experience Siphon** (`xp_siphon`)
Picking up an experience orb heals you `amount` × its value.

---

## Enchanting

These work on the vanilla enchanting table and on Apotheosis' table.

**Enchanting Discount** (`enchanting_discount`)
Enchanting costs `amount` less (capped at 90%, never below 1). Vanilla: levels; Apotheosis: experience.

**Bonus Enchantment** (`bonus_enchantment`, field `chance`)
After you enchant an item, `chance` to add one more random enchantment that fits the item (no curses or treasure enchantments). A chance above 1 adds one for sure plus a chance for another (`1.5` = 1 for sure, 50% for a second). Use `item_condition` to limit it to some items.

**Enchantment Amplification** (`enchantment_amplification`, field `chance`)
After you enchant an item, each of its enchantments has `chance` to go up by one level, even above the max level. Same rule for values above 1.

---

## Pets

A pet is any entity owned by you: tamed animals, and summons from mods that expose an owner or summoner (Iron's Spells summons included), followed up to 4 owners deep.

**Pack Leader** (`pack_leader`, `radius` [16], `max_pets` [5])
+`amount` damage for each of your pets within `radius` blocks, up to `max_pets` pets.

**Pack Hide** (`pack_hide`, `radius` [16], `max_pets` [5])
-`amount` damage taken from mobs for each of your pets within `radius` blocks, up to `max_pets` pets, capped at 60%.

**Feral Bond** (`feral_bond`)
You heal `amount` of the damage your pets deal.

**Blood Pact** (`blood_pact`)
Your pets heal `amount` of the damage they deal.

**Den** (`den`, `interval` [2 s], `radius` [24])
Every `interval` seconds, your pets within `radius` blocks heal `amount` of their max health.

**Prey** (`prey`, `duration` [6 s])
Your hits mark the target for `duration` seconds. Your pets deal +`amount` damage to marked targets.

**Pincer** (`pincer`, `duration` [3 s])
When your pet hits a target, you deal +`amount` damage to it for `duration` seconds.

**Frenzy** (`frenzy`, `max_stacks` [10], `window` [8 s])
Each kill by one of your pets gives a stack, up to `max_stacks`. Each stack gives you and your pets +`amount` damage. The stacks are lost after `window` seconds without a pet kill.

**Guardian** (`guardian`, `threshold` [35%], `cooldown` [30 s])
When a mob's hit takes you under `threshold`% health, you get absorption equal to `amount` × the total max health of your pets within 16 blocks (capped at your max health). Then waits `cooldown` seconds.

**Beast Shield** (`beast_shield`, `radius` [16])
Your nearest pet within `radius` blocks takes `amount` (capped at 50%) of the damage mobs deal to you, and you take that much less.

**Vengeance** (`vengeance`, `radius` [32], `duration` [5 s])
When a mob hits you, your pets within `radius` blocks that have no target attack it, and they deal +`amount` damage to it for `duration` seconds.

**Savage** (`savage`, `multiplier` [2])
Your pets' hits have `amount` chance to deal `multiplier` times damage.

**Spiked Collar** (`spiked_collar`)
Whatever hits your pet takes `amount` of that damage back as thorns damage.

**Howl** (`howl`, `interval` [20 s], `duration` [6 s])
While you are in combat, every `interval` seconds your pets within 24 blocks get Strength and Speed for `duration` seconds. `amount` is the effect level (flat: `1` = level I, `2` = level II).

**Undying Bond** (`undying`, `cooldown` [60 s], `duration` [6 s])
When one of your pets would die, it survives with `amount` of its max health and gets Resistance IV and Regeneration II for `duration` seconds. Each pet can do this once every `cooldown` seconds.

---

## Apotheosis (optional)

**Gem Power** (`gem_power`)
The stats of gems socketed in your equipped items are `amount` stronger (`0.2` = +20%). Use `item_condition` to limit it to some items (for example only the chestplate).

---

## Value providers

These can be used as a PST `player_multiplier` (or anywhere PST takes a value provider) to make a bonus grow.

- `skillascension:stacks` (`stack` [`trophies`]): the value of a named stack from `config/skillascension/stacks.json` (see [datapacks.md](datapacks.md#stacks)).
- `skillascension:experience_level` (`max` [0 = no cap]): your experience level.
- `skillascension:gems_amount` (`item_condition`): number of gems socketed in the matching items (Apotheosis).
- `skillascension:curse_amount` (`item_condition`): number of curses on the matching items.
- `skillascension:spell_power_sum` (`min` [1.25]): adds up your spell power in every school that is at least `min` (Iron's Spells).
