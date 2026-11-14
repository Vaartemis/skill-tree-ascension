package dev.skillascension.client;

import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;

public final class PstPathHint {
   private PstPathHint() {
   }

   public static int distance(PassiveSkillTree tree, PassiveSkill target) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && tree != null && target != null && PlayerSkillsProvider.hasSkills(player)) {
         Set<ResourceLocation> inTree = new HashSet<>(tree.getSkillIds());
         Set<ResourceLocation> learned = new HashSet<>();

         for (PassiveSkill s : PlayerSkillsProvider.get(player).getPlayerSkills()) {
            if (inTree.contains(s.getId())) {
               learned.add(s.getId());
            }
         }

         if (learned.contains(target.getId())) {
            return -1;
         } else {
            Map<ResourceLocation, Set<ResourceLocation>> adj = new HashMap<>();

            for (ResourceLocation id : inTree) {
               PassiveSkill sx = SkillsReloader.getSkillById(id);
               if (sx != null) {
                  for (List<ResourceLocation> l : List.of(sx.getDirectConnections(), sx.getLongConnections())) {
                     for (ResourceLocation o : l) {
                        adj.computeIfAbsent(id, k -> new HashSet<>()).add(o);
                        adj.computeIfAbsent(o, k -> new HashSet<>()).add(id);
                     }
                  }

                  for (ResourceLocation o : sx.getOneWayConnections()) {
                     adj.computeIfAbsent(id, k -> new HashSet<>()).add(o);
                  }
               }
            }

            Map<ResourceLocation, Integer> dist = new HashMap<>();
            ArrayDeque<ResourceLocation> queue = new ArrayDeque<>();
            if (learned.isEmpty()) {
               for (ResourceLocation idx : inTree) {
                  PassiveSkill sx = SkillsReloader.getSkillById(idx);
                  if (sx != null && sx.isStartingPoint()) {
                     dist.put(idx, 1);
                     queue.add(idx);
                  }
               }
            } else {
               for (ResourceLocation idxx : learned) {
                  dist.put(idxx, 0);
                  queue.add(idxx);
               }
            }

            while (!queue.isEmpty()) {
               ResourceLocation cur = queue.poll();
               int d = dist.get(cur);
               if (cur.equals(target.getId())) {
                  return d;
               }

               for (ResourceLocation o : adj.getOrDefault(cur, Set.of())) {
                  if (inTree.contains(o) && !dist.containsKey(o)) {
                     dist.put(o, d + 1);
                     queue.add(o);
                  }
               }
            }

            return -1;
         }
      } else {
         return -1;
      }
   }

   public static int freePoints() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && PlayerSkillsProvider.hasSkills(player) ? PlayerSkillsProvider.get(player).getSkillPoints() : 0;
   }
}
