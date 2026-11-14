package dev.skillascension.client;

import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Client copy of the player's points, plus what is left after the skills queued on the open screen. */
public final class PstClientPoints {
    private static volatile Map<ResourceLocation, Integer> points = Map.of();

    private PstClientPoints() {
    }

    public static void set(Map<ResourceLocation, Integer> newPoints) {
        points = Map.copyOf(newPoints);
    }

    public static int synced(ResourceLocation type) {
        return points.getOrDefault(type, 0);
    }

    public static int left(SkillTreeWidgets widgets, ResourceLocation type) {
        if (PointTypes.NATIVE.equals(type)) return widgets.skillPoints;
        int left = synced(type);
        java.util.Set<ResourceLocation> seen = new java.util.HashSet<>();
        for (ResourceLocation id : widgets.newlyLearnedSkills) {
            PassiveSkill skill = SkillsReloader.getSkillById(id);
            if (skill == null) continue;
            NodeRule.Resolved rule = NodeRules.of(skill);
            if (!type.equals(rule.points)) continue;
            boolean learned = PstRankQueue.learnedOnScreen(id) || !seen.add(id);
            left -= learned ? rule.levelCost : rule.cost;
        }
        return left;
    }

    public static boolean canAfford(SkillTreeWidgets widgets, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        return left(widgets, rule.points) >= rule.cost;
    }

    public static boolean canAffordLevel(SkillTreeWidgets widgets, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        return left(widgets, rule.points) >= rule.levelCost;
    }

    public static void queue(SkillTreeWidgets widgets, PassiveSkill skill, boolean alreadyLearned) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (rule.isNative()) widgets.skillPoints -= alreadyLearned ? rule.levelCost : rule.cost;
        widgets.newlyLearnedSkills.add(skill.getId());
    }

    public static boolean unqueue(SkillTreeWidgets widgets, PassiveSkill skill) {
        int i = widgets.newlyLearnedSkills.lastIndexOf(skill.getId());
        if (i < 0) return false;
        widgets.newlyLearnedSkills.remove(i);
        NodeRule.Resolved rule = NodeRules.of(skill);
        boolean wasLevel = PstRankQueue.learnedOnScreen(skill.getId()) || widgets.newlyLearnedSkills.contains(skill.getId());
        if (rule.isNative()) widgets.skillPoints += wasLevel ? rule.levelCost : rule.cost;
        return true;
    }

    /** Any non-native point type with points left (so PST still highlights learnable skills at 0 skill points). */
    public static boolean anyExtraLeft(SkillTreeWidgets widgets) {
        for (PointType type : PointTypes.extra()) if (left(widgets, type.id) > 0) return true;
        return false;
    }
}
