package dev.skillascension.pst;

import dev.skillascension.pst.bonus.EventBonus;
import daripher.skilltree.skill.SkillBonusProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.ForgeRegistries;

public final class ClassEvents {
   private static final UUID ANVIL_UUID = UUID.fromString("6f1c3b9e-3f5e-4d8f-9a51-0c2a7a4e8b11");
   private static final Map<UUID, float[]> FURY = new HashMap<>();
   private static final Map<UUID, float[]> TIDE = new HashMap<>();
   private static final Map<LivingEntity, float[]> MARKS = new WeakHashMap<>();
   private static final Map<LivingEntity, float[]> SHRED = new WeakHashMap<>();
   private static final Map<UUID, Long> STORM_CD = new HashMap<>();
   private static final Map<UUID, Long> SHELL_CD = new HashMap<>();
   private static boolean striking;
   private static MobEffect wet;

   private ClassEvents() {
   }

   private static boolean isThrownTrident(Entity e) {
      if (e instanceof ThrownTrident) {
         return true;
      } else if (!(e instanceof AbstractArrow)) {
         return false;
      } else {
         ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
         return id != null && id.getPath().contains("trident") ? true : e.getClass().getSimpleName().contains("Trident");
      }
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
            c = c == 0.0F ? e.c() : (kind == EventBonus.Kind.HIGH_TIDE ? Math.min(c, e.c()) : Math.max(c, e.c()));
         }
      }

      return any && a > 0.0F ? new float[]{a, b, c} : null;
   }

   private static boolean allyNear(ServerPlayer player, float radius) {
      double r2 = (double)(radius * radius);

      for (Player p : player.level().players()) {
         if (p != player && p.isAlive() && !p.isSpectator() && p.distanceToSqr(player) <= r2) {
            return true;
         }
      }

      return false;
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onHurt(LivingHurtEvent event) {
      LivingEntity target = event.getEntity();
      if (!target.level().isClientSide) {
         float amount = event.getAmount();
         long now = dev.skillascension.common.Clock.now();

         try {
            float[] mark = MARKS.get(target);
            if (mark != null) {
               if ((float)now < mark[1]) {
                  amount *= 1.0F + mark[0];
               } else {
                  MARKS.remove(target);
               }
            }

            if (event.getSource().getEntity() instanceof ServerPlayer player && player != target) {
               boolean melee = event.getSource().getDirectEntity() == player;
               if (melee) {
                  float[] fury = get(player, EventBonus.Kind.FURY);
                  if (fury != null) {
                     float[] st = FURY.computeIfAbsent(player.getUUID(), k -> new float[2]);
                     float decay = fury[2] > 0.0F ? fury[2] : 3.0F;
                     if ((float)(now - (long)st[1]) > decay * 1000.0F) {
                        st[0] = 0.0F;
                     }

                     amount *= 1.0F + fury[0] * st[0];
                     st[0] = Math.min(fury[1] > 0.0F ? fury[1] : 10.0F, st[0] + 1.0F);
                     st[1] = (float)now;
                  }

                  float[] anvil = get(player, EventBonus.Kind.ANVIL);
                  if (anvil != null) {
                     shred(target, anvil, now);
                  }
               }

               if (event.getSource().getDirectEntity() instanceof Projectile && mark == null) {
                  float[] hm = get(player, EventBonus.Kind.HUNTERS_MARK);
                  if (hm != null) {
                     float dur = hm[1] > 0.0F ? hm[1] : 8.0F;
                     MARKS.put(target, new float[]{hm[0], (float)now + dur * 1000.0F});
                     target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int)(dur * 20.0F), 0, false, false));
                  }
               }

               if (isThrownTrident(event.getSource().getDirectEntity())) {
                  float[] hp = get(player, EventBonus.Kind.HARPOON);
                  if (hp != null) {
                     amount *= 1.0F + hp[0];
                     double res = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                     Vec3 d = player.position().subtract(target.position());
                     double pull = (hp[1] > 0.0F ? (double)hp[1] : 0.6) * Math.max(0.0, 1.0 - res);
                     if (pull > 0.01 && d.lengthSqr() > 4.0) {
                        Vec3 v = d.normalize().scale(pull);
                        target.push(v.x, Math.min(0.4, v.y + 0.15), v.z);
                        target.hurtMarked = true;
                     }
                  }
               }

               if (!striking && dev.skillascension.compat.Compat.isSpellDamage(event.getSource()) && player.isInWaterRainOrBubble()) {
                  float[] sc = get(player, EventBonus.Kind.STORMCALLER);
                  Long cd = STORM_CD.get(player.getUUID());
                  if (sc != null && (cd == null || now >= cd) && player.getRandom().nextFloat() < Math.min(0.5F, sc[0])) {
                     STORM_CD.put(player.getUUID(), now + 1000L);
                     LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(target.level());
                     if (bolt != null) {
                        bolt.moveTo(target.getX(), target.getY(), target.getZ());
                        bolt.setVisualOnly(true);
                        target.level().addFreshEntity(bolt);
                     }

                     striking = true;

                     try {
                        target.invulnerableTime = 0;
                        target.hurt(player.damageSources().indirectMagic(player, player), sc[1] > 0.0F ? sc[1] : 6.0F);
                     } finally {
                        striking = false;
                     }
                  }
               }

               float[] st = TIDE.get(player.getUUID());
               if (st != null && st[0] > 0.0F) {
                  float[] tide = get(player, EventBonus.Kind.HIGH_TIDE);
                  if (tide != null) {
                     amount *= 1.0F + tide[0] * st[0];
                  }
               }

               float[] bond = get(player, EventBonus.Kind.BOND);
               if (bond != null && allyNear(player, bond[1] > 0.0F ? bond[1] : 16.0F)) {
                  amount *= 1.0F + Math.min(0.25F, bond[0]);
               }
            }

            if (target instanceof ServerPlayer victim) {
               float[] bond = get(victim, EventBonus.Kind.BOND);
               if (bond != null && allyNear(victim, bond[1] > 0.0F ? bond[1] : 16.0F)) {
                  amount *= 1.0F - Math.min(0.25F, bond[0]);
               }
            }
         } catch (Throwable var19) {
         }

         event.setAmount(amount);
      }
   }

   private static void shred(LivingEntity target, float[] anvil, long now) {
      AttributeInstance armor = target.getAttribute(Attributes.ARMOR);
      if (armor != null) {
         float[] s = SHRED.computeIfAbsent(target, k -> new float[3]);
         if ((float)now > s[1]) {
            s[0] = 0.0F;
         }

         s[0] = Math.min(anvil[1] > 0.0F ? anvil[1] : 10.0F, s[0] + 1.0F);
         s[1] = (float)now + (anvil[2] > 0.0F ? anvil[2] : 5.0F) * 1000.0F;
         s[2] = anvil[0];
         armor.removeModifier(ANVIL_UUID);
         armor.addTransientModifier(new AttributeModifier(ANVIL_UUID, "Skill anvil", (double)(-s[0] * s[2]), Operation.ADDITION));
      }
   }

   @SubscribeEvent
   public static void onDamaged(LivingDamageEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !(event.getAmount() <= 0.0F)) {
         try {
            float[] shell = get(player, EventBonus.Kind.SHELL);
            if (shell != null) {
               if (wet == null) {
                  wet = (MobEffect)ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("traveloptics", "wet"));
               }

               long now = dev.skillascension.common.Clock.now();
               Long cd = SHELL_CD.get(player.getUUID());
               boolean wetNow = player.isInWaterRainOrBubble() || wet != null && player.hasEffect(wet);
               if (wetNow && (cd == null || now >= cd)) {
                  SHELL_CD.put(player.getUUID(), now + 1000L);
                  float max = shell[1] > 0.0F ? shell[1] : 10.0F;
                  float cur = player.getAbsorptionAmount();
                  if (cur < max) {
                     player.setAbsorptionAmount(Math.min(max, cur + shell[0]));
                  }
               }
            }
         } catch (Throwable var9) {
         }

         float[] forge = get(player, EventBonus.Kind.LIVING_FORGE);
         if (forge == null) {
            return;
         }

         int fix = Math.max(1, Math.round(forge[0]));

         for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = player.getItemBySlot(slot);
            if (s.isDamageableItem() && s.getDamageValue() > 0) {
               s.setDamageValue(Math.max(0, s.getDamageValue() - fix));
            }
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onUseFinish(Finish event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         ItemStack var10 = event.getItem();

         try {
            if (var10.getItem() instanceof PotionItem) {
               float[] sp = get(player, EventBonus.Kind.SHARED_POTION);
               if (sp == null) {
                  return;
               }

               List<MobEffectInstance> effects = PotionUtils.getMobEffects(var10);

               for (Player p : near(player, sp[1] > 0.0F ? sp[1] : 8.0F)) {
                  for (MobEffectInstance e : effects) {
                     if (e.getEffect().isInstantenous()) {
                        e.getEffect().applyInstantenousEffect(player, player, p, e.getAmplifier(), (double)Math.min(1.0F, sp[0]));
                     } else {
                        p.addEffect(
                           new MobEffectInstance(
                              e.getEffect(), Math.max(20, (int)((float)e.getDuration() * sp[0])), e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()
                           ),
                           player
                        );
                     }
                  }
               }

               return;
            }

            FoodProperties food = var10.getFoodProperties(player);
            if (food == null) {
               return;
            }

            float[] feast = get(player, EventBonus.Kind.FEAST);
            if (feast == null) {
               return;
            }

            float heal = (float)food.getNutrition() * feast[0];

            for (Player p : near(player, feast[1] > 0.0F ? feast[1] : 8.0F)) {
               p.heal(heal);
               ((ServerLevel)p.level()).sendParticles(ParticleTypes.HEART, p.getX(), p.getEyeY() + 0.3, p.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
            }
         } catch (Throwable var9) {
         }
      }
   }

   private static List<Player> near(ServerPlayer player, float radius) {
      double r2 = (double)(radius * radius);
      List<Player> out = new ArrayList<>();

      for (Player p : player.level().players()) {
         if (p != player && p.isAlive() && !p.isSpectator() && p.distanceToSqr(player) <= r2) {
            out.add(p);
         }
      }

      return out;
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player) {
         if (dev.skillascension.compat.Compat.IRONS) {
            dev.skillascension.compat.Compat.ironsTick(player);
         }

         if (player.tickCount % 20 == 11) {
            try {
               float[] tide = get(player, EventBonus.Kind.HIGH_TIDE);
               if (tide == null) {
                  TIDE.remove(player.getUUID());
                  return;
               }

               float interval = tide[2] > 0.0F ? tide[2] : 10.0F;
               float[] st = TIDE.computeIfAbsent(player.getUUID(), k -> new float[3]);
               if (player.isInWaterRainOrBubble()) {
                  st[2] = 0.0F;
                  if (++st[1] >= interval) {
                     st[1] = 0.0F;
                     float max = tide[1] > 0.0F ? tide[1] : 5.0F;
                     if (st[0] < max) {
                        st[0]++;
                        player.displayClientMessage(
                           Component.translatable("skillascension.high_tide.stack", new Object[]{(int)st[0], (int)max}).withStyle(ChatFormatting.AQUA), true
                        );
                     }
                  }
               } else if (st[0] > 0.0F && ++st[2] >= interval) {
                  st[0] = 0.0F;
                  st[1] = 0.0F;
                  st[2] = 0.0F;
               }
            } catch (Throwable var7) {
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTick(ServerTickEvent event) {
      if (event.phase == Phase.END && !SHRED.isEmpty()) {
         long now = dev.skillascension.common.Clock.now();
         Iterator<Entry<LivingEntity, float[]>> it = SHRED.entrySet().iterator();

         while (it.hasNext()) {
            Entry<LivingEntity, float[]> e = it.next();
            if ((float)now > e.getValue()[1] || !e.getKey().isAlive()) {
               AttributeInstance armor = e.getKey().getAttribute(Attributes.ARMOR);
               if (armor != null) {
                  armor.removeModifier(ANVIL_UUID);
               }

               it.remove();
            }
         }
      }
   }

   @SubscribeEvent
   public static void onFished(ItemFishedEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         try {
            float[] xp = get(player, EventBonus.Kind.CATCH_XP);
            if (xp != null) {
               player.giveExperiencePoints(Math.max(1, Math.round(xp[0])));
            }
         } catch (Throwable var3) {
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      UUID id = event.getEntity().getUUID();
      FURY.remove(id);
      TIDE.remove(id);
      STORM_CD.remove(id);
      SHELL_CD.remove(id);
   }
}
