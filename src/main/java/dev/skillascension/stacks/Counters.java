package dev.skillascension.stacks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;

public final class Counters {
   private static final int COMBAT_TICKS = 200;
   private static final Map<UUID, Integer> LAST_COMBAT = new HashMap<>();
   private static Map<String, Map<String, Double>> formulas;

   private Counters() {
   }

   public static void bump(ServerPlayer var0, String var1, double var2, double var4) {
      if (!(var2 <= 0.0)) {
         double var6 = var2 * DeedFilter.factor(var0, var4);
         if (var6 > 0.0) {
            EternalStacks.set(var0, "c." + var1, EternalStacks.get(var0, "c." + var1) + var6);
         }
      }
   }

   static void markCombat(Player var0) {
      LAST_COMBAT.put(var0.getUUID(), var0.tickCount);
   }

   public static boolean inCombat(Player var0) {
      Integer var1 = LAST_COMBAT.get(var0.getUUID());
      return var1 != null && var0.tickCount - var1 < 200;
   }

   public static Double formula(Player var0, String var1) {
      Map<String, Double> var2 = formulas().get(var1);
      if (var2 == null) {
         return null;
      } else {
         double var3 = 0.0;

         for (Entry<String, Double> var6 : var2.entrySet()) {
            var3 += EternalStacks.get(var0, (String)var6.getKey()) * var6.getValue();
         }

         return var3;
      }
   }

   public static int preload() {
      return formulas().size();
   }

   private static synchronized Map<String, Map<String, Double>> formulas() {
      if (formulas != null) {
         return formulas;
      } else {
         LinkedHashMap var0 = new LinkedHashMap();
         Path var1 = FMLPaths.CONFIGDIR.get().resolve("skillascension").resolve("stacks.json");
         Gson var2 = new GsonBuilder().setPrettyPrinting().create();

         try {
            if (!Files.exists(var1)) {
               Files.createDirectories(var1.getParent());

               try (BufferedWriter var3 = Files.newBufferedWriter(var1, StandardCharsets.UTF_8)) {
                  var2.toJson(defaults(), var3);
               }
            }

            try (BufferedReader var17 = Files.newBufferedReader(var1, StandardCharsets.UTF_8)) {
               JsonObject var4 = (JsonObject)var2.fromJson(var17, JsonObject.class);

               for (Entry var6 : var4.entrySet()) {
                  if (!((String)var6.getKey()).startsWith("_")) {
                     LinkedHashMap var7 = new LinkedHashMap();

                     for (Entry var9 : ((JsonElement)var6.getValue()).getAsJsonObject().entrySet()) {
                        var7.put((String)var9.getKey(), ((JsonElement)var9.getValue()).getAsDouble());
                     }

                     var0.put((String)var6.getKey(), var7);
                  }
               }

            }
         } catch (Exception var16) {
            StacksLog.LOG.error("Could not read config/skillascension/stacks.json, using the built-in defaults: {}", var16.toString());
            var0.clear();
            var0.putAll(fromJson(defaults()));
         }

         StacksLog.checkFormulas(var0);
         formulas = var0;
         return var0;
      }
   }

   private static Map<String, Map<String, Double>> fromJson(JsonObject var0) {
      LinkedHashMap var1 = new LinkedHashMap();

      for (Entry var3 : var0.entrySet()) {
         if (!((String)var3.getKey()).startsWith("_")) {
            LinkedHashMap var4 = new LinkedHashMap();

            for (Entry var6 : ((JsonElement)var3.getValue()).getAsJsonObject().entrySet()) {
               var4.put((String)var6.getKey(), ((JsonElement)var6.getValue()).getAsDouble());
            }

            var1.put((String)var3.getKey(), var4);
         }
      }

      return var1;
   }

   private static JsonObject defaults() {
      JsonObject var0 = new JsonObject();
      var0.addProperty(
         "_comment",
         "Named stacks = weighted sums of counters (c.*) or raw stacks (trophies, essence:<school>). Nodes use {\"type\":\"skillascension:stacks\",\"stack\":\"<name>\"}. Reload: restart."
      );
      var0.add("fury", obj("c.hits_melee", 0.02, "c.damage_melee", 0.002, "trophies", 1));
      var0.add("scars", obj("c.hits_taken", 0.05, "c.damage_taken", 0.005, "c.damage_blocked", 0.005));
      var0.add("marksman", obj("c.arrows_shot", 0.02, "c.arrow_damage", 0.002, "c.kills_ranged", 1));
      var0.add("spellcraft", obj("c.spells_cast", 0.05, "c.mana_spent", 0.001, "essence:*", 1));
      var0.add("slayer", obj("c.kills", 1, "c.kills_boss", 50));
      var0.add("angler", obj("c.fish_caught", 0.5, "c.treasure_caught", 2));
      var0.add("tide", obj("c.kills_wet", 1, "c.fish_caught", 0.2));
      var0.add("beast", obj("c.pet_kills", 1, "c.pet_damage", 0.002, "c.species_tamed", 10, "c.tames", 1));
      var0.add("kinship", obj("c.pet_kills", 1, "c.pet_damage", 0.002, "c.pet_hits", 0.02));
      var0.add("menagerie", obj("c.species_tamed", 10, "c.tames", 1, "c.pet_damage_taken", 0.002));
      var0.add("runes", obj("c.enchant_levels", 0.2, "c.anvil_enchants", 1));
      var0.add("forge", obj("c.items_crafted", 0.05, "c.items_smelted", 0.02, "c.anvil_uses", 0.5));
      var0.add("alchemy", obj("c.potions_brewed", 0.5, "c.debuffs_applied", 0.2, "c.potions_drunk", 0.3));
      var0.add("chef", obj("c.food_cooked", 0.2, "c.food_eaten", 0.3));
      return var0;
   }

   private static JsonObject obj(Object... var0) {
      JsonObject var1 = new JsonObject();

      for (int var2 = 0; var2 < var0.length; var2 += 2) {
         var1.addProperty((String)var0[var2], ((Number)var0[var2 + 1]).doubleValue());
      }

      return var1;
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onHurt(LivingHurtEvent var0) {
      LivingEntity var1 = var0.getEntity();
      if (!var1.level().isClientSide && !(var0.getAmount() <= 0.0F)) {
         DamageSource var2 = var0.getSource();
         Entity var3 = var2.getEntity();
         if (var3 instanceof ServerPlayer var4 && !(var3 instanceof FakePlayer) && var1 instanceof Enemy && DeedFilter.threat(var4, var1)) {
            markCombat(var4);
            double var5 = (double)Math.min(var0.getAmount(), var1.getHealth());
            bump(var4, "hits_dealt", 1.0, 0.02);
            bump(var4, "damage_dealt", var5, 0.0);
            String var7 = EternalStacks.school(var2);
            if (var7 != null) {
               bump(var4, "hits_spell", 1.0, 0.0);
               bump(var4, "damage_spell", var5, 0.0);
               bump(var4, "damage_spell:" + var7, var5, 0.0);
            } else if (var2.is(DamageTypeTags.IS_PROJECTILE)) {
               bump(var4, "hits_ranged", 1.0, 0.0);
               bump(var4, "damage_ranged", var5, 0.0);
               if (var2.getDirectEntity() instanceof AbstractArrow) {
                  bump(var4, "arrow_damage", var5, 0.0);
               }
            } else if (var2.getDirectEntity() == var4) {
               bump(var4, "hits_melee", 1.0, 0.0);
               bump(var4, "damage_melee", var5, 0.0);
            }
         }

         if (var1 instanceof ServerPlayer var8 && !(var1 instanceof FakePlayer) && var3 instanceof LivingEntity && var3 instanceof Enemy) {
            markCombat(var8);
            bump(var8, "hits_taken", 1.0, 0.02);
            bump(var8, "damage_taken", (double)Math.min(var0.getAmount(), var8.getMaxHealth()), 0.0);
         }
      }
   }

   @SubscribeEvent
   public static void onBlock(ShieldBlockEvent var0) {
      if (var0.getEntity() instanceof ServerPlayer var1 && !(var1 instanceof FakePlayer) && var0.getDamageSource().getEntity() instanceof Enemy) {
         markCombat(var1);
         bump(var1, "blocks", 1.0, 0.02);
         bump(var1, "damage_blocked", (double)var0.getBlockedDamage(), 0.0);
      }
   }

   @SubscribeEvent
   public static void onLoose(ArrowLooseEvent var0) {
      if (var0.getEntity() instanceof ServerPlayer var1 && !(var1 instanceof FakePlayer) && inCombat(var1) && var0.getCharge() >= 5) {
         bump(var1, "arrows_shot", 1.0, 0.02);
      }
   }

   static void countKill(ServerPlayer var0, DamageSource var1, String var2, boolean var3) {
      bump(var0, "kills", 1.0, 0.0);
      if (var3) {
         bump(var0, "kills_boss", 1.0, 0.0);
      }

      if (var0.isInWaterRainOrBubble()) {
         bump(var0, "kills_wet", 1.0, 0.0);
      }

      if (var2 != null) {
         bump(var0, "kills_spell", 1.0, 0.0);
      } else if (var1.is(DamageTypeTags.IS_PROJECTILE)) {
         bump(var0, "kills_ranged", 1.0, 0.0);
      } else if (var1.getDirectEntity() == var0) {
         bump(var0, "kills_melee", 1.0, 0.0);
      }
   }

   public static void hookSpells() {
      try {
         Class var0 = Class.forName("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent");
         Method var1 = var0.getMethod("getEntity");
         Method var2 = var0.getMethod("getManaCost");
         Method var3 = var0.getMethod("getSchoolType");
         Consumer var4 = var3x -> {
            try {
               Object var4x = var1.invoke(var3x);
               if (var4x instanceof ServerPlayer var5x && !(var4x instanceof FakePlayer) && inCombat(var5x)) {
                  bump(var5x, "spells_cast", 1.0, 0.02);
                  Object var6x = var3.invoke(var3x);
                  if (var6x != null) {
                     Object var7 = var6x.getClass().getMethod("getId").invoke(var6x);
                     String var8 = var7.toString();
                     var8 = var8.substring(var8.indexOf(58) + 1);
                     bump(var5x, "spells_cast:" + var8, 1.0, 0.0);
                  }

                  if (var2.invoke(var3x) instanceof Number var12) {
                     bump(var5x, "mana_spent", var12.doubleValue(), 0.0);
                  }

                  return;
               }
            } catch (Exception var9) {
               StacksLog.warnOnce("spellcast", "Iron's SpellOnCastEvent handler failed, spell counters may be wrong: {}", var9.toString());
            }
         };
         MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, var0, var4);
         StacksLog.spellHook = true;
         StacksLog.LOG.info("Hooked Iron's Spellbooks SpellOnCastEvent (spells_cast, mana_spent)");
      } catch (ClassNotFoundException var5) {
         StacksLog.LOG.warn("Iron's Spellbooks not found: spells_cast and mana_spent will stay at 0");
      } catch (Throwable var6) {
         StacksLog.LOG.error("Could not hook Iron's SpellOnCastEvent (API changed?): {}", var6.toString());
      }
   }
}
