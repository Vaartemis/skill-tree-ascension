package dev.skillascension.stacks;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.living.MobEffectEvent.Added;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemSmeltedEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class ClassCounters {
   private ClassCounters() {
   }

   private static ServerPlayer real(Entity var0) {
      if (var0 instanceof ServerPlayer var1 && !(var0 instanceof FakePlayer)) {
         return var1;
      }

      return null;
   }

   private static ServerPlayer petOwner(Entity var0) {
      if (var0 instanceof OwnableEntity var1 && !(var0 instanceof ServerPlayer)) {
         return real(var1.getOwner());
      }

      return null;
   }

   private static boolean fightingPet(LivingEntity var0, Entity var1, ServerPlayer var2) {
      if (var0 instanceof Mob var3 && (var3.getTarget() == var1 || var3.getTarget() == var2)) {
         return true;
      }

      LivingEntity var4 = var0.getLastHurtMob();
      return var4 == var1 || var4 == var2;
   }

   @SubscribeEvent
   public static void onFish(ItemFishedEvent var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null) {
         Counters.bump(var1, "fish_caught", 1.0, 0.05);

         for (ItemStack var3 : var0.getDrops()) {
            if (!var3.is(ItemTags.FISHES)) {
               Counters.bump(var1, "treasure_caught", 1.0, 0.0);
               break;
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onPetHurt(LivingHurtEvent var0) {
      LivingEntity var1 = var0.getEntity();
      if (!var1.level().isClientSide && !(var0.getAmount() <= 0.0F)) {
         DamageSource var2 = var0.getSource();
         Entity var3 = var2.getEntity();
         ServerPlayer var4 = petOwner(var3);
         if (var4 != null && var1 instanceof Enemy && fightingPet(var1, var3, var4)) {
            Counters.markCombat(var4);
            Counters.bump(var4, "pet_hits", 1.0, 0.02);
            Counters.bump(var4, "pet_damage", (double)Math.min(var0.getAmount(), var1.getHealth()), 0.0);
         }

         ServerPlayer var5 = petOwner(var1);
         if (var5 != null && var3 instanceof Enemy) {
            Counters.bump(var5, "pet_damage_taken", (double)Math.min(var0.getAmount(), var1.getMaxHealth()), 0.02);
         }
      }
   }

   @SubscribeEvent
   public static void onPetKill(LivingDeathEvent var0) {
      LivingEntity var1 = var0.getEntity();
      if (!var1.level().isClientSide && var1 instanceof Enemy) {
         Entity var2 = var0.getSource().getEntity();
         ServerPlayer var3 = petOwner(var2);
         if (var3 != null && fightingPet(var1, var2, var3)) {
            Counters.bump(var3, "pet_kills", EternalStacks.weight(var1), 1.0);
         }
      }
   }

   @SubscribeEvent
   public static void onTame(AnimalTameEvent var0) {
      ServerPlayer var1 = real(var0.getTamer());
      if (var1 != null) {
         Counters.bump(var1, "tames", 1.0, 0.5);
         String var2 = "c.tamed:" + EntityType.getKey(var0.getAnimal().getType());
         if (EternalStacks.get(var1, var2) <= 0.0) {
            EternalStacks.set(var1, var2, 1.0);
            EternalStacks.set(var1, "c.species_tamed", EternalStacks.get(var1, "c.species_tamed") + 1.0);
         }
      }
   }

   @SubscribeEvent
   public static void onLevels(LevelChange var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null && var0.getLevels() < 0 && var1.containerMenu != null) {
         String var2 = var1.containerMenu.getClass().getName().toLowerCase();
         if (var2.contains("enchant")) {
            Counters.bump(var1, "enchant_levels", (double)(-var0.getLevels()), 0.05);
         }
      }
   }

   @SubscribeEvent
   public static void onAnvil(AnvilRepairEvent var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null) {
         Counters.bump(var1, "anvil_uses", 1.0, 0.05);
         if (var0.getOutput().isEnchanted()) {
            Counters.bump(var1, "anvil_enchants", 1.0, 0.0);
         }
      }
   }

   @SubscribeEvent
   public static void onCraft(ItemCraftedEvent var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null && !var0.getCrafting().isEmpty()) {
         Counters.bump(var1, "items_crafted", 1.0, 0.01);
         if (var0.getCrafting().isEdible()) {
            Counters.bump(var1, "food_cooked", 1.0, 0.0);
         }
      }
   }

   @SubscribeEvent
   public static void onSmelt(ItemSmeltedEvent var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null) {
         int var2 = Math.max(1, var0.getSmelting().getCount());
         Counters.bump(var1, "items_smelted", (double)var2, 0.01);
         if (var0.getSmelting().isEdible()) {
            Counters.bump(var1, "food_cooked", (double)var2, 0.0);
         }
      }
   }

   @SubscribeEvent
   public static void onBrew(PlayerBrewedPotionEvent var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null) {
         Counters.bump(var1, "potions_brewed", 1.0, 0.02);
      }
   }

   @SubscribeEvent
   public static void onDebuff(Added var0) {
      ServerPlayer var1 = real(var0.getEffectSource());
      MobEffectInstance var2 = var0.getEffectInstance();
      if (var1 != null && var2 != null && !var2.getEffect().isBeneficial() && var0.getEntity() instanceof Enemy) {
         Counters.bump(var1, "debuffs_applied", 1.0, 0.02);
      }
   }

   @SubscribeEvent
   public static void onConsume(Finish var0) {
      ServerPlayer var1 = real(var0.getEntity());
      if (var1 != null) {
         ItemStack var2 = var0.getItem();
         if (var2.getItem() instanceof PotionItem) {
            Counters.bump(var1, "potions_drunk", 1.0, 0.05);
         } else if (var2.isEdible()) {
            Counters.bump(var1, "food_eaten", 1.0, 0.05);
         }
      }
   }
}
