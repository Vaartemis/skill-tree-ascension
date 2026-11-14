package dev.skillascension.pst;

import dev.skillascension.pst.bonus.EventBonus;
import dev.skillascension.net.AscensionNet;
import daripher.skilltree.skill.SkillBonusProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class CombatEvents {
   private static final int MANA_FLOW_MAX = 10;
   private static final float WAR_CRY_RADIUS = 8.0F;
   public static final Map<UUID, double[]> FLOW = new HashMap<>();
   public static final Map<UUID, Long> CHANNEL = new HashMap<>();
   public static final Map<UUID, double[]> CHAIN = new HashMap<>();
   public static final Map<UUID, String> LAST_SCHOOL = new HashMap<>();
   private static final Map<UUID, Long> COUNTER = new HashMap<>();
   private static final Map<UUID, double[]> STREAK = new HashMap<>();
   private static final Map<UUID, Long> CRY_CD = new HashMap<>();
   private static final Map<UUID, float[]> CRY_BUFF = new HashMap<>();

   private CombatEvents() {
   }

   public static float[] get(ServerPlayer player, EventBonus.Kind kind) {
      float a = 0.0F;
      float b = 0.0F;
      float c = 0.0F;
      boolean any = false;

      for (EventBonus e : SkillBonusProvider.getSkillBonuses(player, EventBonus.class)) {
         if (e.kind() == kind) {
            any = true;
            a += e.amount(player);
            b = Math.max(b, e.b());
            boolean lower = kind == EventBonus.Kind.WAR_CRY;
            c = c == 0.0F ? e.c() : (lower ? Math.min(c, e.c()) : Math.max(c, e.c()));
            if (kind == EventBonus.Kind.MANA_FLOW) {
               b = b == 0.0F ? e.b() : Math.min(b, e.b());
            }
         }
      }

      return any && a > 0.0F ? new float[]{a, b, c} : null;
   }

   @SubscribeEvent
   public static void onBlock(ShieldBlockEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && get(player, EventBonus.Kind.COUNTER) != null) {
         float[] c = get(player, EventBonus.Kind.COUNTER);
         COUNTER.put(player.getUUID(), dev.skillascension.common.Clock.now() + (long)((c[1] > 0.0F ? c[1] : 3.0F) * 1000.0F));
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onHurt(LivingHurtEvent event) {
      LivingEntity target = event.getEntity();
      if (!target.level().isClientSide && event.getSource().getEntity() instanceof ServerPlayer player && player != target) {
         try {
            long now = dev.skillascension.common.Clock.now();
            UUID id = player.getUUID();
            float mult = 1.0F;
            if (dev.skillascension.compat.Compat.isSpellDamage(event.getSource())) {
               float[] flow = get(player, EventBonus.Kind.MANA_FLOW);
               double[] st = FLOW.get(id);
               if (flow != null && st != null && (double)now <= st[2]) {
                  mult = (float)((double)mult + (double)flow[0] * st[1]);
               }

               Long until = CHANNEL.get(id);
               float[] oc = get(player, EventBonus.Kind.OVERCHANNEL);
               if (oc != null && until != null && now <= until) {
                  mult += oc[0];
               }

               float[] ch = get(player, EventBonus.Kind.SCHOOL_CHAIN);
               double[] cs = CHAIN.get(id);
               if (ch != null && cs != null && (double)now <= cs[1]) {
                  mult = (float)((double)mult + (double)ch[0] * cs[0]);
               }
            }

            if (event.getSource().getDirectEntity() == player) {
               Long untilx = COUNTER.remove(id);
               float[] c = get(player, EventBonus.Kind.COUNTER);
               if (c != null && untilx != null && now <= untilx) {
                  mult += c[0];
                  if (player.level() instanceof ServerLevel sl) {
                     sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                  }
               }
            }

            float[] ex = get(player, EventBonus.Kind.EXECUTE);
            if (ex != null && target.getHealth() < target.getMaxHealth() * (ex[1] > 0.0F ? ex[1] : 25.0F) / 100.0F) {
               mult += ex[0];
            }

            float[] ks = get(player, EventBonus.Kind.KILL_STREAK);
            double[] streak = STREAK.get(id);
            if (ks != null && streak != null && (double)now <= streak[1]) {
               mult = (float)((double)mult + (double)ks[0] * streak[0]);
            }

            float[] cry = CRY_BUFF.get(id);
            if (cry != null) {
               if ((float)now <= cry[1]) {
                  mult += cry[0];
               } else {
                  CRY_BUFF.remove(id);
               }
            }

            if (mult != 1.0F) {
               event.setAmount(event.getAmount() * mult);
            }
         } catch (Throwable var13) {
         }
      }
   }

   @SubscribeEvent
   public static void onKill(LivingDeathEvent event) {
      if (event.getSource().getEntity() instanceof ServerPlayer player && event.getEntity() != player) {
         float[] ks = get(player, EventBonus.Kind.KILL_STREAK);
         if (ks == null) {
            return;
         }

         long now = dev.skillascension.common.Clock.now();
         double[] st = STREAK.computeIfAbsent(player.getUUID(), k -> new double[2]);
         if ((double)now > st[1]) {
            st[0] = 0.0;
         }

         st[0] = Math.min(ks[1] > 0.0F ? (double)ks[1] : 10.0, st[0] + 1.0);
         st[1] = (double)(now + (long)((ks[2] > 0.0F ? ks[2] : 8.0F) * 1000.0F));
         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onHeal(LivingHealEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
         float[] oh = get(player, EventBonus.Kind.OVERHEAL);
         if (oh == null) {
            return;
         }

         float excess = player.getHealth() + event.getAmount() - player.getMaxHealth();
         if (excess <= 0.0F) {
            return;
         }

         float max = oh[1] > 0.0F ? oh[1] : 10.0F;
         player.setAbsorptionAmount(Math.min(max, Math.max(player.getAbsorptionAmount(), 0.0F) + excess * oh[0]));
         return;
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onDamaged(LivingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !event.isCanceled()) {
         float[] cry = get(player, EventBonus.Kind.WAR_CRY);
         if (cry == null) {
            return;
         }

         float after = player.getHealth() - event.getAmount();
         if (!(after <= 0.0F) && !(after >= player.getMaxHealth() * (cry[1] > 0.0F ? cry[1] : 50.0F) / 100.0F)) {
            long now = dev.skillascension.common.Clock.now();
            Long cd = CRY_CD.get(player.getUUID());
            if (cd != null && now < cd) {
               return;
            }

            float cooldown = cry[2] > 0.0F ? cry[2] : 60.0F;
            CRY_CD.put(player.getUUID(), now + (long)(cooldown * 1000.0F));
            AscensionNet.sendCooldown(player, EventBonus.Kind.WAR_CRY.id, (int)(cooldown * 20.0F));
            double r2 = 64.0;

            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0))) {
               if (e != player && !(e.distanceToSqr(player) > r2)) {
                  if (e instanceof Player ally) {
                     CRY_BUFF.put(ally.getUUID(), new float[]{cry[0], (float)(now + 8000L)});
                  } else if (e instanceof Enemy) {
                     e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                     e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 1));
                  }
               }
            }

            CRY_BUFF.put(player.getUUID(), new float[]{cry[0], (float)(now + 8000L)});
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.2F);
            if (player.level() instanceof ServerLevel sl) {
               sl.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY(1.0), player.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            }

            return;
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      FLOW.remove(id);
      CHANNEL.remove(id);
      CHAIN.remove(id);
      LAST_SCHOOL.remove(id);
      COUNTER.remove(id);
      STREAK.remove(id);
      CRY_CD.remove(id);
      CRY_BUFF.remove(id);
   }
}
