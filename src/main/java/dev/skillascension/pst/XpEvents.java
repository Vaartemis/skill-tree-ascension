package dev.skillascension.pst;

import dev.skillascension.pst.bonus.EventBonus;
import dev.skillascension.net.AscensionNet;
import daripher.skilltree.skill.SkillBonusProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class XpEvents {
   private static final Map<UUID, Long> NEXT_INTEREST = new HashMap<>();
   private static final Map<UUID, Long> NEXT_LAST_PAGE = new HashMap<>();
   private static final Map<UUID, Long> NEXT_SURGE = new HashMap<>();

   private XpEvents() {
   }

   private static List<EventBonus> of(ServerPlayer player, EventBonus.Kind kind) {
      List<EventBonus> out = new ArrayList<>();

      for (EventBonus b : SkillBonusProvider.getSkillBonuses(player, EventBonus.class)) {
         if (b.kind() == kind) {
            out.add(b);
         }
      }

      return out;
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 7) {
         try {
            float amount = 0.0F;
            float interval = Float.MAX_VALUE;

            for (EventBonus b : of(player, EventBonus.Kind.XP_INTEREST)) {
               amount += b.amount(player);
               interval = Math.min(interval, Math.max(1.0F, b.b()));
            }

            if (amount <= 0.0F) {
               NEXT_INTEREST.remove(player.getUUID());
               return;
            }

            long now = System.currentTimeMillis();
            Long next = NEXT_INTEREST.get(player.getUUID());
            if (next == null) {
               NEXT_INTEREST.put(player.getUUID(), now + (long)(interval * 1000.0F));
               return;
            }

            if (now < next) {
               return;
            }

            NEXT_INTEREST.put(player.getUUID(), now + (long)(interval * 1000.0F));
            int points = Math.max(1, Math.round(amount * (float)player.experienceLevel));
            player.giveExperiencePoints(points);
         } catch (Throwable var8) {
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUp(LevelChange event) {
      if (event.getLevels() > 0 && event.getEntity() instanceof ServerPlayer player) {
         long var10 = System.currentTimeMillis();
         Long next = NEXT_SURGE.get(player.getUUID());
         if (next == null || var10 >= next) {
            float heal = 0.0F;
            float absorption = 0.0F;
            float duration = 0.0F;

            for (EventBonus b : of(player, EventBonus.Kind.LEVEL_SURGE)) {
               heal += b.amount(player);
               absorption = Math.max(absorption, b.b());
               duration = Math.max(duration, b.c());
            }

            if (!(heal <= 0.0F) || !(absorption <= 0.0F)) {
               NEXT_SURGE.put(player.getUUID(), var10 + 3000L);
               if (heal > 0.0F) {
                  player.heal(player.getMaxHealth() * Math.min(1.0F, heal));
               }

               if (absorption > 0.0F && duration > 0.0F) {
                  int amplifier = Math.max(0, Math.round(absorption / 4.0F) - 1);
                  player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int)(duration * 20.0F), amplifier, false, false, true));
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onDamage(LivingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !(event.getAmount() < player.getHealth())) {
         float hp = 0.0F;
         float cost = Float.MAX_VALUE;
         float cooldown = Float.MAX_VALUE;

         for (EventBonus b : of(player, EventBonus.Kind.LAST_PAGE)) {
            hp += b.amount(player);
            cost = Math.min(cost, b.b());
            cooldown = Math.min(cooldown, b.c());
         }

         if (hp <= 0.0F) {
            return;
         }

         long now = System.currentTimeMillis();
         Long next = NEXT_LAST_PAGE.get(player.getUUID());
         int levels = (int)Math.ceil((double)cost);
         if (next != null && now < next) {
            return;
         }

         if (player.experienceLevel < levels) {
            player.displayClientMessage(Component.translatable("skillascension.last_page.no_levels", new Object[]{levels}).withStyle(ChatFormatting.DARK_RED), true);
            return;
         }

         NEXT_LAST_PAGE.put(player.getUUID(), now + (long)(cooldown * 1000.0F));
         AscensionNet.sendCooldown(player, EventBonus.Kind.LAST_PAGE.id, (int)(cooldown * 20.0F));
         player.giveExperienceLevels(-levels);
         event.setCanceled(true);
         player.setHealth(Math.max(1.0F, player.getMaxHealth() * Math.min(1.0F, hp)));
         player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, false, true));
         player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.3F);
         player.displayClientMessage(Component.translatable("skillascension.last_page.used", new Object[]{levels, (int)cooldown}).withStyle(ChatFormatting.LIGHT_PURPLE), true);
         return;
      }
   }

   @SubscribeEvent
   public static void onKill(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player) {
         float chance = 0.0F;
         float over = 0.0F;

         for (EventBonus b : of(player, EventBonus.Kind.LEARNING_BLADE)) {
            chance += b.amount(player);
            over = Math.max(over, b.b());
         }

         if (!(chance <= 0.0F) && !(player.getRandom().nextFloat() >= chance)) {
            ItemStack weapon = player.getMainHandItem();
            if (weapon.isEmpty()) {
               return;
            }

            Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(weapon));
            List<Enchantment> growable = new ArrayList<>();

            for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
               Enchantment ench = e.getKey();
               if (!ench.isCurse() && ench.getMaxLevel() > 1 && e.getValue() < ench.getMaxLevel() + (int)over) {
                  growable.add(ench);
               }
            }

            if (growable.isEmpty()) {
               return;
            }

            Enchantment pick = growable.get(player.getRandom().nextInt(growable.size()));
            int level = enchants.get(pick) + 1;
            enchants.put(pick, level);
            EnchantmentHelper.setEnchantments(enchants, weapon);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.displayClientMessage(Component.translatable("skillascension.learning_blade.grew", new Object[]{pick.getFullname(level)}).withStyle(ChatFormatting.AQUA), true);
            return;
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      NEXT_INTEREST.remove(event.getEntity().getUUID());
      NEXT_SURGE.remove(event.getEntity().getUUID());
   }
}
