package dev.skillascension.stacks;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class EternalStacks {
   private static final String PERSISTED = "PlayerPersisted";
   private static final String NBT_KEY = "skillascension_stacks";
   private static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "bosses"));
   private static final int[] MILESTONES = new int[]{100, 500, 1000, 5000};
   private static boolean shockwave;

   private EternalStacks() {
   }

   public static double get(Player var0, String var1) {
      if (!var1.startsWith("c.") && !var1.contains(":")) {
         Double var2 = Counters.formula(var0, var1);
         if (var2 != null) {
            return var2;
         }
      }

      CompoundTag var8 = var0.getPersistentData().getCompound("PlayerPersisted").getCompound("skillascension_stacks");
      if (var1.endsWith(":*")) {
         String var3 = var1.substring(0, var1.length() - 1);
         double var4 = 0.0;

         for (String var7 : var8.getAllKeys()) {
            if (var7.startsWith(var3)) {
               var4 += var8.getDouble(var7);
            }
         }

         return var4;
      } else {
         return var8.getDouble(var1);
      }
   }

   public static void set(ServerPlayer var0, String var1, double var2) {
      CompoundTag var4 = var0.getPersistentData();
      CompoundTag var5 = var4.getCompound("PlayerPersisted");
      CompoundTag var6 = var5.getCompound("skillascension_stacks");
      var6.putDouble(var1, Math.max(0.0, var2));
      var5.put("skillascension_stacks", var6);
      var4.put("PlayerPersisted", var5);
   }

   public static void add(ServerPlayer var0, String var1, Component var2, double var3) {
      double var5 = var3 * DeedFilter.factor(var0);
      if (!(var5 <= 0.0)) {
         double var7 = get(var0, var1);
         double var9 = var7 + var5;
         set(var0, var1, var9);
         var0.displayClientMessage(
            Component.translatable("skillascension.stacks.gain", new Object[]{var2, fmt(var5), fmt(var9)}).withStyle(new ChatFormatting[]{ChatFormatting.GRAY}), true
         );

         for (int var14 : MILESTONES) {
            if (var7 < (double)var14 && var9 >= (double)var14) {
               var0.displayClientMessage(
                  Component.translatable("skillascension.stacks.milestone", new Object[]{var2, var14}).withStyle(new ChatFormatting[]{ChatFormatting.GOLD}), false
               );
            }
         }
      }
   }

   private static String fmt(double var0) {
      return var0 == Math.floor(var0) ? String.valueOf((long)var0) : String.format("%.1f", var0);
   }

   static double weight(LivingEntity var0) {
      if (var0.getType().is(BOSSES)) {
         return 100.0;
      } else {
         return var0.getMaxHealth() >= 100.0F ? 5.0 : 1.0;
      }
   }

   static String school(DamageSource var0) {
      Optional var1 = var0.typeHolder().unwrapKey();
      if (var1.isEmpty()) {
         return null;
      } else {
         ResourceLocation var2 = ((ResourceKey)var1.get()).location();
         String var3 = var2.getPath();
         if (!var3.endsWith("_magic")) {
            return null;
         } else {
            String var4 = var2.getNamespace();
            return !var4.equals("irons_spellbooks") && !var4.equals("traveloptics") && !var4.equals("alshanex_familiars") && !var4.contains("spell")
               ? null
               : var3.substring(0, var3.length() - "_magic".length());
         }
      }
   }

   @SubscribeEvent
   public static void onDeath(LivingDeathEvent var0) {
      LivingEntity var1 = var0.getEntity();
      if (!var1.level().isClientSide && var1 instanceof Enemy) {
         DamageSource var2 = var0.getSource();
         Entity var3 = var2.getEntity();
         if (!(var3 instanceof ServerPlayer var4) || var3 instanceof FakePlayer) {
            return;
         }

         if (DeedFilter.threat(var4, var1)) {
            double var5 = weight(var1);
            String var7 = school(var2);
            Counters.markCombat(var4);
            Counters.countKill(var4, var2, var7, var5 >= 100.0);
            if (var7 != null) {
               add(var4, "essence:" + var7, Component.translatable("skillascension.stacks.essence." + var7), var5);
            } else {
               if (var2.getDirectEntity() == var4 && !var2.is(DamageTypeTags.IS_PROJECTILE)) {
                  add(var4, "trophies", Component.translatable("skillascension.stacks.trophies"), var5);
                  if (!shockwave && get(var4, "trophies") >= 100.0) {
                     trophyShockwave(var4, var1);
                  }
               }
            }
         }
      }
   }

   private static void trophyShockwave(ServerPlayer var0, LivingEntity var1) {
      float var2 = (float)(var0.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5);
      List<LivingEntity> var3 = var1.level().getEntitiesOfClass(LivingEntity.class, var1.getBoundingBox().inflate(3.0));
      shockwave = true;

      try {
         for (LivingEntity var5 : var3) {
            if (var5 != var1 && var5 instanceof Enemy && var5.isAlive()) {
               var5.hurt(var0.damageSources().playerAttack(var0), var2);
            }
         }
      } finally {
         shockwave = false;
      }
   }

   @SubscribeEvent
   public static void onServerStarted(ServerStartedEvent var0) {
      int var1 = Counters.preload();
      StacksLog.LOG.info("Stacks ready: {} named stacks, spell hook {}, shockwave at 100 trophies", var1, StacksLog.spellHook ? "ON" : "OFF");
   }

   @SubscribeEvent
   public static void onCommands(RegisterCommandsEvent var0) {
      var0.getDispatcher()
         .register(
            (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("skillascension").requires(var0x -> var0x.hasPermission(2)))
               .then(
                  ((LiteralArgumentBuilder)Commands.literal("stacks")
                        .then(
                           Commands.literal("get")
                              .then(
                                 Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("key", StringArgumentType.string()).executes(var0x -> {
                                       ServerPlayer var1 = EntityArgument.getPlayer(var0x, "player");
                                       String var2 = StringArgumentType.getString(var0x, "key");
                                       double var3 = get(var1, var2);
                                       ((CommandSourceStack)var0x.getSource()).sendSuccess(() -> Component.literal(var2 + " = " + fmt(var3)), false);
                                       return (int)var3;
                                    }))
                              )
                        ))
                     .then(
                        Commands.literal("set")
                           .then(
                              Commands.argument("player", EntityArgument.player())
                                 .then(
                                    Commands.argument("key", StringArgumentType.string())
                                       .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0)).executes(var0x -> {
                                          ServerPlayer var1 = EntityArgument.getPlayer(var0x, "player");
                                          String var2 = StringArgumentType.getString(var0x, "key");
                                          double var3 = DoubleArgumentType.getDouble(var0x, "value");
                                          set(var1, var2, var3);
                                          ((CommandSourceStack)var0x.getSource()).sendSuccess(() -> Component.literal(var2 + " = " + fmt(var3)), true);
                                          return 1;
                                       }))
                                 )
                           )
                     )
               )
         );
   }
}
