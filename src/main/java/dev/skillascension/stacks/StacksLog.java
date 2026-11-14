package dev.skillascension.stacks;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class StacksLog {
   public static final Logger LOG = LogManager.getLogger("SkillAscension/Stacks");
   private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();
   static boolean spellHook;
   static int formulaCount;
   static final Set<String> COUNTERS = Set.of(
      "anvil_enchants",
      "anvil_uses",
      "arrow_damage",
      "arrows_shot",
      "blocks",
      "damage_blocked",
      "damage_dealt",
      "damage_melee",
      "damage_ranged",
      "damage_spell",
      "damage_taken",
      "debuffs_applied",
      "enchant_levels",
      "fish_caught",
      "food_cooked",
      "food_eaten",
      "hits_dealt",
      "hits_melee",
      "hits_ranged",
      "hits_spell",
      "hits_taken",
      "items_crafted",
      "items_smelted",
      "kills",
      "kills_boss",
      "kills_melee",
      "kills_ranged",
      "kills_spell",
      "kills_wet",
      "mana_spent",
      "pet_damage",
      "pet_damage_taken",
      "pet_hits",
      "pet_kills",
      "potions_brewed",
      "potions_drunk",
      "spells_cast",
      "species_tamed",
      "tames",
      "treasure_caught"
   );

   private StacksLog() {
   }

   public static void warnOnce(String var0, String var1, Object... var2) {
      if (WARNED.add(var0)) {
         LOG.warn(var1, var2);
      }
   }

   static boolean known(String var0) {
      if (var0.equals("trophies") || var0.startsWith("essence:")) {
         return true;
      } else if (var0.startsWith("c.")) {
         String var1 = var0.substring(2);
         int var2 = var1.indexOf(58);
         return var2 <= 0
            ? COUNTERS.contains(var1)
            : var1.startsWith("spells_cast:") || var1.startsWith("damage_spell:") || var1.startsWith("hits_spell:") || var1.startsWith("tamed:");
      } else {
         return false;
      }
   }

   static void checkFormulas(Map<String, Map<String, Double>> var0) {
      int var1 = 0;

      for (Entry<String, Map<String, Double>> var3 : var0.entrySet()) {
         for (String var5 : var3.getValue().keySet()) {
            if (!known(var5) && !var5.endsWith("*")) {
               LOG.warn("stacks.json: stack '{}' uses unknown key '{}' (typo? it will always be 0)", var3.getKey(), var5);
               var1++;
            }
         }
      }

      formulaCount = var0.size();
      LOG.info("Loaded {} named stacks from config/skillascension/stacks.json ({} unknown keys)", var0.size(), var1);
   }
}
