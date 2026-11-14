package dev.skillascension.pst;

import dev.skillascension.pst.bonus.EventBonus;
import daripher.skilltree.skill.SkillBonusProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class DeathKnowledgeEvents {
   private static final String KEPT_XP = "skillascension_memento_xp";
   private static final UUID SHADOWSTEP_UUID = UUID.fromString("3b7f0e52-9d4a-4c61-8f2e-6a1d5c9e7b40");
   private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
   private static final Map<UUID, List<Object[]>> GIVE_BACK = new HashMap<>();

   private DeathKnowledgeEvents() {
   }

   public static float amount(Player var0, EventBonus.Kind var1) {
      if (!(var0 instanceof ServerPlayer)) {
         return 0.0F;
      } else {
         float var2 = 0.0F;

         try {
            for (EventBonus var4 : SkillBonusProvider.getSkillBonuses(var0, EventBonus.class)) {
               if (var4.kind() == var1) {
                  var2 += var4.amount(var0);
               }
            }

            return var2;
         } catch (Throwable var5) {
            return 0.0F;
         }
      }
   }

   public static boolean roll(Player var0, float var1) {
      return var1 > 0.0F && var0.getRandom().nextFloat() < var1;
   }

   public static void giveBack(Player var0, ItemStack var1, InteractionHand var2) {
      GIVE_BACK.computeIfAbsent(var0.getUUID(), var0x -> new ArrayList<>()).add(new Object[]{var1, var2});
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onDeath(LivingDeathEvent var0) {
      if (!var0.isCanceled() && var0.getEntity() instanceof ServerPlayer var1) {
         if (!var1.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            float var5 = Math.min(1.0F, amount(var1, EventBonus.Kind.MEMENTO_MORI));
            if (!(var5 <= 0.0F)) {
               int var3 = totalExperience(var1);
               int var4 = Math.round((float)var3 * var5);
               if (var4 > 0) {
                  ((IForgeEntity)var1).getPersistentData().putInt("skillascension_memento_xp", var4);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onXpDrop(LivingExperienceDropEvent var0) {
      if (var0.getEntity() instanceof ServerPlayer var1 && ((IForgeEntity)var1).getPersistentData().contains("skillascension_memento_xp")) {
         float var3 = Math.min(1.0F, amount(var1, EventBonus.Kind.MEMENTO_MORI));
         var0.setDroppedExperience(Math.round((float)var0.getDroppedExperience() * (1.0F - var3)));
      }
   }

   @SubscribeEvent
   public static void onClone(Clone var0) {
      if (var0.isWasDeath()) {
         Player var1 = var0.getOriginal();
         if (((IForgeEntity)var1).getPersistentData().contains("skillascension_memento_xp")) {
            int var2 = ((IForgeEntity)var1).getPersistentData().getInt("skillascension_memento_xp");
            ((IForgeEntity)var1).getPersistentData().remove("skillascension_memento_xp");
            ((IForgeEntity)var0.getEntity()).getPersistentData().remove("skillascension_memento_xp");
            if (var2 > 0) {
               var0.getEntity().giveExperiencePoints(var2);
            }
         }
      }
   }

   private static int totalExperience(Player var0) {
      int var1 = var0.experienceLevel;
      long var2;
      if (var1 <= 16) {
         var2 = (long)var1 * (long)var1 + 6L * (long)var1;
      } else if (var1 <= 31) {
         var2 = (long)(2.5 * (double)var1 * (double)var1 - 40.5 * (double)var1 + 360.0);
      } else {
         var2 = (long)(4.5 * (double)var1 * (double)var1 - 162.5 * (double)var1 + 2220.0);
      }

      var2 += (long)Math.round(var0.experienceProgress * (float)var0.getXpNeededForNextLevel());
      return (int)Math.min(2147483647L, var2);
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent var0) {
      if (var0.phase == Phase.END && var0.player instanceof ServerPlayer var1) {
         List<Object[]> var7 = GIVE_BACK.remove(var1.getUUID());
         if (var7 != null) {
            for (Object[] var4 : var7) {
               ItemStack var5 = (ItemStack)var4[0];
               InteractionHand var6 = (InteractionHand)var4[1];
               if (var6 != null && var1.getItemInHand(var6).isEmpty()) {
                  var1.setItemInHand(var6, var5);
               } else if (!var1.getInventory().add(var5)) {
                  var1.drop(var5, false);
               }
            }
         }

         if (var1.tickCount % 5 == 0) {
            AttributeInstance var8 = var1.getAttribute(Attributes.MOVEMENT_SPEED);
            if (var8 != null) {
               float var9 = var1.isCrouching() ? amount(var1, EventBonus.Kind.SHADOWSTEP) : 0.0F;
               AttributeModifier var10 = var8.getModifier(SHADOWSTEP_UUID);
               if (var9 <= 0.0F) {
                  if (var10 != null) {
                     var8.removeModifier(SHADOWSTEP_UUID);
                  }
               } else if (var10 == null || Math.abs(var10.getAmount() - (double)var9) > 1.0E-4) {
                  if (var10 != null) {
                     var8.removeModifier(SHADOWSTEP_UUID);
                  }

                  var8.addTransientModifier(new AttributeModifier(SHADOWSTEP_UUID, "Shadowstep", (double)var9, Operation.MULTIPLY_TOTAL));
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onVisibility(LivingVisibilityEvent var0) {
      if (var0.getEntity() instanceof ServerPlayer var1) {
         float var3 = amount(var1, EventBonus.Kind.SHADOWVEIL);
         if (var3 > 0.0F) {
            var0.modifyVisibility(Math.max(0.1, (double)(1.0F - var3)));
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onTotem(LivingUseTotemEvent var0) {
      if (!var0.isCanceled() && var0.getEntity() instanceof ServerPlayer var1) {
         if (roll(var1, amount(var1, EventBonus.Kind.WITCH_DOCTOR))) {
            ItemStack var3 = var0.getTotem().copy();
            var3.setCount(1);
            giveBack(var1, var3, var0.getHandHolding());
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onDrops(LivingDropsEvent var0) {
      LivingEntity var1 = var0.getEntity();
      if (var0.getSource().getEntity() instanceof ServerPlayer var2) {
         if (var1.getMobType() == MobType.UNDEAD && !var1.getType().is(BOSSES)) {
            if (roll(var2, amount(var2, EventBonus.Kind.TREASURE_SEEKER))) {
               ArrayList var7 = new ArrayList();

               for (ItemEntity var5 : var0.getDrops()) {
                  ItemEntity var6 = new ItemEntity(var1.level(), var5.getX(), var5.getY(), var5.getZ(), var5.getItem().copy());
                  var6.setDefaultPickUpDelay();
                  var7.add(var6);
               }

               var0.getDrops().addAll(var7);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent var0) {
      GIVE_BACK.remove(var0.getEntity().getUUID());
   }
}
