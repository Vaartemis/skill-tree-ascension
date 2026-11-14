package dev.skillascension.pets;

import dev.skillascension.pst.bonus.PetBonus;
import dev.skillascension.pst.bonus.PetBonus.Kind;
import dev.skillascension.stacks.Counters;
import dev.skillascension.stacks.StacksLog;
import daripher.skilltree.init.PSTSkillBonuses;
import daripher.skilltree.skill.SkillBonusProvider;
import daripher.skilltree.skill.bonus.SkillBonus.Serializer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.RegisterEvent;

public final class PetEvents {
   static final Map<Kind, Serializer> SERIALIZERS = new EnumMap<>(Kind.class);
   private static final Map<Class<?>, Method> SUMMONER = new HashMap<>();
   private static final Map<Entity, Map<UUID, Long>> PREY = new WeakHashMap<>();
   private static final Map<Entity, Map<UUID, Long>> PINCER = new WeakHashMap<>();
   private static final Map<Entity, Map<UUID, Long>> VENGEANCE = new WeakHashMap<>();
   private static final Map<UUID, long[]> FRENZY = new HashMap<>();
   private static final Map<UUID, Long> GUARD_CD = new HashMap<>();
   private static boolean busy;

   private PetEvents() {
   }

   public static void onRegister(RegisterEvent var0) {
      ResourceKey var1 = ResourceKey.createRegistryKey(PSTSkillBonuses.REGISTRY_ID);
      var0.register(
         var1,
         var0x -> {
            for (Kind var4 : Kind.values()) {
               dev.skillascension.pst.bonus.PetBonus.Serializer var5 = new dev.skillascension.pst.bonus.PetBonus.Serializer(
                  var4, () -> SERIALIZERS.get(var4)
               );
               SERIALIZERS.put(var4, var5);
               var0x.register(new ResourceLocation("skillascension", var4.id), var5);
            }

            StacksLog.LOG.info("Registered {} Beastmaster pet bonuses (skillascension:pack_leader ...)", Kind.values().length);
         }
      );
   }

   public static void init() {
      MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, LivingHurtEvent.class, PetEvents::onHurt);
      MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDeathEvent.class, PetEvents::onDeath);
      MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerTickEvent.class, PetEvents::onTick);
      StacksLog.LOG.info("Beastmaster pet events ON");
   }

   private static Entity ownerOf(Entity var0) {
      if (var0 instanceof OwnableEntity var6) {
         return var6.getOwner();
      } else if (var0 instanceof TraceableEntity var5) {
         return var5.getOwner();
      } else if (var0 instanceof Projectile var4) {
         return var4.getOwner();
      } else {
         Method var1 = SUMMONER.computeIfAbsent(var0.getClass(), var0x -> {
            try {
               Method var1x = var0x.getMethod("getSummoner");
               return Entity.class.isAssignableFrom(var1x.getReturnType()) ? var1x : null;
            } catch (Exception var2) {
               return null;
            }
         });
         if (var1 == null) {
            return null;
         } else {
            try {
               return (Entity)var1.invoke(var0);
            } catch (Exception var3) {
               return null;
            }
         }
      }
   }

   public static ServerPlayer ownerPlayer(Entity var0) {
      if (var0 != null && !(var0 instanceof Player) && !(var0 instanceof Projectile)) {
         Entity var1 = var0;

         for (int var2 = 0; var1 != null && var2 < 4; var2++) {
            var1 = ownerOf(var1);
            if (var1 instanceof ServerPlayer var3 && !(var3 instanceof FakePlayer)) {
               return var3;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   static float[] get(Player var0, Kind var1) {
      float var2 = 0.0F;
      float var3 = 0.0F;
      float var4 = 0.0F;
      boolean var5 = false;

      for (PetBonus var7 : SkillBonusProvider.getSkillBonuses(var0, PetBonus.class)) {
         if (var7.kind() == var1) {
            var5 = true;
            var2 += var7.amount(var0);
            var3 = var1.minB() ? (var3 == 0.0F ? var7.b() : Math.min(var3, var7.b())) : Math.max(var3, var7.b());
            var4 = Math.max(var4, var7.c());
         }
      }

      return var5 && var2 > 0.0F ? new float[]{var2, var3, var4} : null;
   }

   static List<LivingEntity> pets(ServerPlayer var0, double var1) {
      ArrayList var3 = new ArrayList();

      for (LivingEntity var5 : var0.level().getEntitiesOfClass(LivingEntity.class, var0.getBoundingBox().inflate(var1), var1x -> var1x != var0 && var1x.isAlive())) {
         if (ownerPlayer(var5) == var0) {
            var3.add(var5);
         }
      }

      return var3;
   }

   private static long now(Entity var0) {
      return var0.level().getGameTime();
   }

   private static void mark(Map<Entity, Map<UUID, Long>> var0, Entity var1, ServerPlayer var2, float var3) {
      var0.computeIfAbsent(var1, var0x -> new HashMap<>()).put(var2.getUUID(), now(var1) + (long)(var3 * 20.0F));
   }

   private static boolean marked(Map<Entity, Map<UUID, Long>> var0, Entity var1, ServerPlayer var2) {
      Map var3 = (Map)var0.get(var1);
      Long var4 = var3 == null ? null : (Long)var3.get(var2.getUUID());
      return var4 != null && var4 >= now(var1);
   }

   private static int frenzy(ServerPlayer var0, float var1) {
      long[] var2 = FRENZY.get(var0.getUUID());
      if (var2 == null) {
         return 0;
      } else if (now(var0) - var2[1] > (long)(var1 * 20.0F)) {
         FRENZY.remove(var0.getUUID());
         return 0;
      } else {
         return (int)var2[0];
      }
   }

   private static void onHurt(LivingHurtEvent var0) {
      if (!busy) {
         LivingEntity var1 = var0.getEntity();
         if (!var1.level().isClientSide && !(var0.getAmount() <= 0.0F)) {
            try {
               Entity var2;
               float var3;
               label300: {
                  busy = true;
                  var2 = var0.getSource().getEntity();
                  var3 = var0.getAmount();
                  if (var2 instanceof ServerPlayer var4 && !(var4 instanceof FakePlayer) && ownerPlayer(var1) != var4) {
                     float[] var20;
                     if ((var20 = get(var4, Kind.PACK_LEADER)) != null) {
                        int var35 = Math.min(pets(var4, (double)var20[1]).size(), (int)var20[2]);
                        var3 *= 1.0F + var20[0] * (float)var35;
                     }

                     if ((var20 = get(var4, Kind.PINCER)) != null && marked(PINCER, var1, var4)) {
                        var3 *= 1.0F + var20[0];
                     }

                     if ((var20 = get(var4, Kind.FRENZY)) != null) {
                        var3 *= 1.0F + var20[0] * (float)frenzy(var4, var20[2]);
                     }

                     if ((var20 = get(var4, Kind.PREY)) != null) {
                        mark(PREY, var1, var4, var20[1]);
                     }
                     break label300;
                  }

                  if (var2 instanceof LivingEntity var5 && !(var2 instanceof Player)) {
                     ServerPlayer var6 = ownerPlayer(var5);
                     if (var6 != null && var1 != var6 && ownerPlayer(var1) != var6) {
                        float[] var7;
                        if ((var7 = get(var6, Kind.PREY)) != null && marked(PREY, var1, var6)) {
                           var3 *= 1.0F + var7[0];
                        }

                        if ((var7 = get(var6, Kind.FRENZY)) != null) {
                           var3 *= 1.0F + var7[0] * (float)frenzy(var6, var7[2]);
                        }

                        if ((var7 = get(var6, Kind.VENGEANCE)) != null && marked(VENGEANCE, var1, var6)) {
                           var3 *= 1.0F + var7[0];
                        }

                        if ((var7 = get(var6, Kind.SAVAGE)) != null && var5.getRandom().nextFloat() < Math.min(1.0F, var7[0])) {
                           var3 *= Math.max(1.0F, var7[1]);
                        }

                        if ((var7 = get(var6, Kind.PINCER)) != null) {
                           mark(PINCER, var1, var6, var7[1]);
                        }

                        float var8 = Math.min(var3, var1.getHealth());
                        if ((var7 = get(var6, Kind.FERAL_BOND)) != null) {
                           var6.heal(var8 * var7[0]);
                        }

                        if ((var7 = get(var6, Kind.BLOOD_PACT)) != null) {
                           var5.heal(var8 * var7[0]);
                        }
                     }
                  }
               }

               if (var1 instanceof ServerPlayer var16 && var2 instanceof LivingEntity var18 && !(var18 instanceof Player) && ownerPlayer(var18) != var16) {
                  float[] var24;
                  if ((var24 = get(var16, Kind.PACK_HIDE)) != null) {
                     int var36 = Math.min(pets(var16, (double)var24[1]).size(), (int)var24[2]);
                     var3 *= 1.0F - Math.min(0.6F, var24[0] * (float)var36);
                  }

                  if ((var24 = get(var16, Kind.BEAST_SHIELD)) != null) {
                     LivingEntity var37 = null;

                     for (LivingEntity var9 : pets(var16, (double)var24[1])) {
                        if (var37 == null || var9.distanceToSqr(var16) < var37.distanceToSqr(var16)) {
                           var37 = var9;
                        }
                     }

                     if (var37 != null) {
                        float var41 = Math.min(0.5F, var24[0]);
                        var37.hurt(var16.damageSources().generic(), var3 * var41);
                        var3 *= 1.0F - var41;
                     }
                  }

                  if ((var24 = get(var16, Kind.VENGEANCE)) != null) {
                     mark(VENGEANCE, var18, var16, var24[2]);

                     for (LivingEntity var42 : pets(var16, (double)var24[1])) {
                        if (var42 instanceof Mob) {
                           Mob var44 = (Mob)var42;
                           if (var44.getTarget() == null) {
                              var44.setTarget(var18);
                           }
                        }
                     }
                  }

                  if ((var24 = get(var16, Kind.GUARDIAN)) != null) {
                     Long var39 = GUARD_CD.get(var16.getUUID());
                     if ((var16.getHealth() - var3) / var16.getMaxHealth() < var24[1] / 100.0F && (var39 == null || var39 <= now(var16))) {
                        float var43 = 0.0F;

                        for (LivingEntity var10 : pets(var16, 16.0)) {
                           var43 += var10.getMaxHealth();
                        }

                        float var46 = Math.min(var16.getMaxHealth(), var43 * var24[0]);
                        if (var46 > 0.0F) {
                           var16.setAbsorptionAmount(Math.max(var16.getAbsorptionAmount(), var46));
                           GUARD_CD.put(var16.getUUID(), now(var16) + (long)(var24[2] * 20.0F));
                        }
                     }
                  }
               }

               if (var2 instanceof LivingEntity var17 && !(var1 instanceof Player)) {
                  ServerPlayer var19 = ownerPlayer(var1);
                  if (var19 != null && var17 != var19 && ownerPlayer(var17) != var19) {
                     float[] var28 = get(var19, Kind.SPIKED_COLLAR);
                     if (var28 != null) {
                        var17.hurt(var1.damageSources().thorns(var1), var3 * var28[0]);
                     }
                  }
               }

               if (var3 != var0.getAmount()) {
                  var0.setAmount(var3);
               }
            } catch (Exception var14) {
               StacksLog.warnOnce("pet_hurt", "Beastmaster pet hurt handler failed: {}", var14.toString());
            } finally {
               busy = false;
            }
         }
      }
   }

   private static void onDeath(LivingDeathEvent var0) {
      try {
         LivingEntity var1 = var0.getEntity();
         ServerPlayer var2 = var1.level().isClientSide ? null : ownerPlayer(var1);
         float[] var3 = var2 == null ? null : get(var2, Kind.UNDYING);
         if (var3 != null) {
            long var4 = now(var1);
            if (var1.getPersistentData().getLong("skillascension_undying") <= var4) {
               var0.setCanceled(true);
               var1.setHealth(Math.max(1.0F, var1.getMaxHealth() * Math.min(1.0F, var3[0])));
               int var10 = (int)(Math.max(1.0F, var3[2]) * 20.0F);
               var1.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, var10, 3));
               var1.addEffect(new MobEffectInstance(MobEffects.REGENERATION, var10, 1));
               var1.getPersistentData().putLong("skillascension_undying", var4 + (long)(Math.max(1.0F, var3[1]) * 20.0F));
               return;
            }
         }

         Entity var9 = var0.getSource().getEntity();
         if (!(var9 instanceof LivingEntity) || var9 instanceof Player || var0.getEntity().level().isClientSide) {
            return;
         }

         ServerPlayer var5 = ownerPlayer(var9);
         if (var5 == null) {
            return;
         }

         float[] var6 = get(var5, Kind.FRENZY);
         if (var6 == null) {
            return;
         }

         int var7 = Math.min((int)Math.max(1.0F, var6[1]), frenzy(var5, var6[2]) + 1);
         FRENZY.put(var5.getUUID(), new long[]{(long)var7, now(var5)});
      } catch (Exception var8) {
         StacksLog.warnOnce("pet_death", "Beastmaster pet death handler failed: {}", var8.toString());
      }
   }

   private static void onTick(PlayerTickEvent var0) {
      if (var0.phase == Phase.END && var0.player instanceof ServerPlayer var1 && var1.tickCount % 20 == 0) {
         try {
            int var9 = var1.tickCount / 20;
            float[] var3;
            if ((var3 = get(var1, Kind.DEN)) != null && var9 % Math.max(1, (int)var3[1]) == 0) {
               for (LivingEntity var5 : pets(var1, (double)var3[2])) {
                  var5.heal(var5.getMaxHealth() * var3[0]);
               }
            }

            if ((var3 = get(var1, Kind.HOWL)) != null && var9 % Math.max(1, (int)var3[1]) == 0 && Counters.inCombat(var1)) {
               int var11 = Math.max(0, Math.round(var3[0]) - 1);
               int var12 = (int)(Math.max(1.0F, var3[2]) * 20.0F);

               for (LivingEntity var7 : pets(var1, 24.0)) {
                  var7.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, var12, var11));
                  var7.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, var12, var11));
               }
            }
         } catch (Exception var8) {
            StacksLog.warnOnce("pet_tick", "Beastmaster pet tick handler failed: {}", var8.toString());
         }
      }
   }
}
