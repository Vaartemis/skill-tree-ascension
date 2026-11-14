package dev.skillascension.stacks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

public final class DeedFilter {
   private static final int AFK_TICKS = 3600;
   private static final double FATIGUE_HALF = 25.0;
   private static final double FATIGUE_RECOVER_SECONDS = 30.0;
   private static final Map<UUID, DeedFilter.Look> LOOKS = new HashMap<>();
   private static final Map<UUID, Map<Long, double[]>> CHUNKS = new HashMap<>();

   private DeedFilter() {
   }

   public static boolean threat(ServerPlayer var0, LivingEntity var1) {
      if (var1 instanceof Mob var2 && var2.getTarget() == var0) {
         return true;
      }

      return var1.getLastHurtMob() == var0;
   }

   public static double factor(ServerPlayer var0) {
      return factor(var0, 1.0);
   }

   public static double factor(ServerPlayer var0, double var1) {
      DeedFilter.Look var3 = LOOKS.get(var0.getUUID());
      if (var3 != null && var0.tickCount - var3.lastMove > 3600) {
         return 0.0;
      } else {
         double var4 = 1.0;
         if (var0.getLastHurtByMob() != null && var0.tickCount - var0.getLastHurtByMobTimestamp() < 200) {
            var4 *= 1.5;
         }

         Map<Long, double[]> var6 = CHUNKS.computeIfAbsent(var0.getUUID(), var0x -> new HashMap<>());
         long var7 = var0.level().getGameTime();
         double[] var9 = var6.computeIfAbsent(var0.chunkPosition().toLong(), var2 -> new double[]{0.0, (double)var7});
         var9[0] = Math.max(0.0, var9[0] - ((double)var7 - var9[1]) / 20.0 / 30.0);
         var9[1] = (double)var7;
         var4 *= Math.pow(0.5, var9[0] / 25.0);
         var9[0] += var1;
         if (var6.size() > 512) {
            var6.clear();
         }

         return var4;
      }
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent var0) {
      if (var0.phase == Phase.END && var0.player instanceof ServerPlayer var1 && var1.tickCount % 20 == 0) {
         DeedFilter.Look var5 = LOOKS.computeIfAbsent(var1.getUUID(), var0x -> new DeedFilter.Look());
         float var3 = var1.getYRot();
         float var4 = var1.getXRot();
         if (Math.abs(var3 - var5.yaw) > 2.0F || Math.abs(var4 - var5.pitch) > 2.0F) {
            var5.lastMove = var1.tickCount;
         }

         var5.yaw = var3;
         var5.pitch = var4;
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent var0) {
      LOOKS.remove(var0.getEntity().getUUID());
      CHUNKS.remove(var0.getEntity().getUUID());
   }

   private static final class Look {
      float yaw;
      float pitch;
      int lastMove;
   }
}
