package dev.skillascension.client;

import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class PstClientRanks {
   private static volatile Map<ResourceLocation, Integer> ranks = Map.of();
   private static volatile int total;

   private PstClientRanks() {
   }

   public static void set(Map<ResourceLocation, Integer> newRanks) {
      ranks = Map.copyOf(newRanks);
      int sum = 0;

      for (int rank : ranks.values()) {
         sum += rank;
      }

      total = sum;
   }

   public static int rank(ResourceLocation skill) {
      return ranks.getOrDefault(skill, 0);
   }

   public static int total() {
      return total;
   }

   /** Add to PST's level (learned skills + points) for the real native level of the local player. */
   public static int levelDelta() {
      net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
      if (player == null || !daripher.skilltree.capability.skill.PlayerSkillsProvider.hasSkills(player)) return 0;
      return dev.skillascension.common.NativeLevel.delta(
         daripher.skilltree.capability.skill.PlayerSkillsProvider.get(player).getPlayerSkills(), PstClientRanks::rank);
   }
}
