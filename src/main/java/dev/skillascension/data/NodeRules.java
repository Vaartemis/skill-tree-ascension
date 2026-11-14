package dev.skillascension.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import daripher.skilltree.data.reloader.SkillTreesReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.SkillAscension;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Finds the {@link NodeRule.Resolved} rule of a skill. Order (later wins, field by field):
 * point type defaults, tags written on the skill, rules matching its tree, rules matching one of its tags,
 * rules naming the skill.
 */
public final class NodeRules {
    private static volatile List<NodeRule> rules = List.of();
    private static final Map<PassiveSkill, NodeRule.Resolved> CACHE = new WeakHashMap<>();
    private static Map<ResourceLocation, PassiveSkillTree> treesSeen;
    private static Map<ResourceLocation, ResourceLocation> treeOfSkill = Map.of();

    private NodeRules() {
    }

    static void load(Map<ResourceLocation, JsonElement> files) {
        List<NodeRule> list = new ArrayList<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (json.has("rules")) {
                    for (JsonElement e : json.getAsJsonArray("rules")) list.add(new NodeRule(e.getAsJsonObject()));
                } else {
                    list.add(new NodeRule(json));
                }
            } catch (Exception e) {
                SkillAscension.LOG.error("Could not read node rules {}: {}", entry.getKey(), e.toString());
            }
        });
        rules = List.copyOf(list);
        invalidate();
    }

    public static synchronized void invalidate() {
        CACHE.clear();
        treesSeen = null;
    }

    public static NodeRule.Resolved of(PassiveSkill skill) {
        if (skill == null) return NodeRule.Resolved.DEFAULT;
        synchronized (NodeRules.class) {
            NodeRule.Resolved cached = CACHE.get(skill);
            if (cached != null) return cached;
            NodeRule.Resolved resolved = resolve(skill);
            CACHE.put(skill, resolved);
            return resolved;
        }
    }

    /** The tree a skill belongs to, or null. */
    public static synchronized ResourceLocation treeOf(ResourceLocation skillId) {
        Map<ResourceLocation, PassiveSkillTree> trees = SkillTreesReloader.getSkillTrees();
        if (trees != treesSeen) {
            Map<ResourceLocation, ResourceLocation> map = new HashMap<>();
            for (PassiveSkillTree tree : trees.values()) for (ResourceLocation id : tree.getSkillIds()) map.putIfAbsent(id, tree.getId());
            treeOfSkill = map;
            treesSeen = trees;
            CACHE.clear();
        }
        return treeOfSkill.get(skillId);
    }

    private static NodeRule.Resolved resolve(PassiveSkill skill) {
        ResourceLocation tree = treeOf(skill.getId());
        List<String> tags = skill.getTags();

        // 1. point type that claims this skill (by tree or by tag)
        ResourceLocation points = PointTypes.NATIVE;
        for (PointType type : PointTypes.all()) {
            if (type.nativePoints) continue;
            if ((tree != null && type.trees.contains(tree)) || (type.skillTag != null && tags.contains(type.skillTag))) {
                points = type.id;
            }
        }
        Integer cost = null;
        boolean repeatable = false;
        int maxLevel = 0;
        Integer levelCost = null;
        int freeEvery = 0;
        double bonusPerLevel = 1.0;
        List<NodeRule.Milestone> milestones = List.of();

        // 2. shortcuts written as skill tags
        for (String tag : tags) {
            if (tag.equalsIgnoreCase("Repeatable")) repeatable = true;
            else if (tag.startsWith("MaxLevel")) maxLevel = parseInt(tag.substring(8), maxLevel);
            else if (tag.startsWith("LevelCost")) levelCost = parseInt(tag.substring(9), 1);
            else if (tag.startsWith("Cost")) cost = parseInt(tag.substring(4), 1);
            else if (tag.startsWith("Points=")) {
                ResourceLocation id = ResourceLocation.tryParse(tag.substring(7));
                if (id != null) points = id;
            }
        }

        // 3. datapack rules, least specific first
        for (int pass = 0; pass < 3; pass++) {
            for (NodeRule rule : rules) {
                boolean match = switch (pass) {
                    case 0 -> tree != null && rule.trees.contains(tree);
                    case 1 -> rule.tag != null && tags.contains(rule.tag);
                    default -> rule.skills.contains(skill.getId());
                };
                if (!match) continue;
                if (rule.points != null) points = rule.points;
                if (rule.cost != null) cost = rule.cost;
                if (rule.repeatable != null) repeatable = rule.repeatable;
                if (rule.maxLevel != null) maxLevel = rule.maxLevel;
                if (rule.levelCost != null) levelCost = rule.levelCost;
                if (rule.freeLevelEvery != null) freeEvery = rule.freeLevelEvery;
                if (rule.bonusPerLevel != null) bonusPerLevel = rule.bonusPerLevel;
                if (rule.milestones != null) milestones = rule.milestones;
            }
        }

        if (!PointTypes.exists(points)) {
            SkillAscension.LOG.warn("Skill {} uses unknown point type {}, using skill points", skill.getId(), points);
            points = PointTypes.NATIVE;
        }
        int finalCost = Math.max(1, cost != null ? cost : PointTypes.get(points).cost);
        int finalLevelCost = Math.max(1, levelCost != null ? levelCost : finalCost);
        return new NodeRule.Resolved(points, finalCost, repeatable, Math.max(0, maxLevel), finalLevelCost, Math.max(0, freeEvery),
                bonusPerLevel, milestones);
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
