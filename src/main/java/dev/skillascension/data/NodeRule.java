package dev.skillascension.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Extra rules for skills (point type, cost, repeatable levels, milestones). Defined in
 * {@code data/<namespace>/skillascension/nodes/<name>.json}. A file holds one rule object or
 * {@code {"rules": [...]}}. Every field is optional; unset fields keep the value from a less
 * specific rule (skill tags, then tree rules, then tag rules, then skill rules).
 */
public final class NodeRule {
    final List<ResourceLocation> skills;
    final List<ResourceLocation> trees;
    final String tag;
    final ResourceLocation points;
    final Integer cost;
    final Boolean repeatable;
    final Integer maxLevel;
    final Integer levelCost;
    final Integer freeLevelEvery;
    final Double bonusPerLevel;
    final List<Milestone> milestones;

    NodeRule(JsonObject json) {
        this.skills = PointType.ids(json, "skills");
        this.trees = PointType.ids(json, "trees");
        this.tag = GsonHelper.getAsString(json, "tag", null);
        this.points = json.has("points") ? new ResourceLocation(GsonHelper.getAsString(json, "points")) : null;
        this.cost = json.has("cost") ? GsonHelper.getAsInt(json, "cost") : null;
        this.repeatable = json.has("repeatable") ? GsonHelper.getAsBoolean(json, "repeatable") : null;
        this.maxLevel = json.has("max_level") ? GsonHelper.getAsInt(json, "max_level") : null;
        this.levelCost = json.has("level_cost") ? GsonHelper.getAsInt(json, "level_cost") : null;
        this.freeLevelEvery = json.has("free_level_every") ? GsonHelper.getAsInt(json, "free_level_every") : null;
        this.bonusPerLevel = json.has("bonus_per_level") ? GsonHelper.getAsDouble(json, "bonus_per_level") : null;
        List<Milestone> list = null;
        if (json.has("milestones")) {
            list = new ArrayList<>();
            for (JsonElement e : GsonHelper.getAsJsonArray(json, "milestones")) list.add(new Milestone(e.getAsJsonObject()));
        }
        this.milestones = list;
    }

    /** Something that happens once when a repeatable skill reaches a level. */
    public static final class Milestone {
        public final int level;
        public final List<String> commands;
        public final Component message;

        Milestone(JsonObject json) {
            this.level = GsonHelper.getAsInt(json, "level");
            List<String> cmds = new ArrayList<>();
            if (json.has("commands")) {
                JsonArray array = GsonHelper.getAsJsonArray(json, "commands");
                for (JsonElement e : array) cmds.add(e.getAsString());
            }
            if (json.has("command")) cmds.add(GsonHelper.getAsString(json, "command"));
            this.commands = List.copyOf(cmds);
            JsonElement m = json.get("message");
            this.message = m == null ? null : m.isJsonPrimitive() ? Component.literal(m.getAsString()) : Component.Serializer.fromJson(m);
        }
    }

    /** The final rule for one skill after merging everything that matches it. */
    public static final class Resolved {
        public static final Resolved DEFAULT = new Resolved(PointTypes.NATIVE, 1, false, 0, 1, 10, 1.0, List.of());

        public final ResourceLocation points;
        public final int cost;
        public final boolean repeatable;
        /** 0 = no limit. Counts the first learn as level 1. */
        public final int maxLevel;
        public final int levelCost;
        /** 0 = no free levels. Only used when there is no level limit. */
        public final int freeLevelEvery;
        public final double bonusPerLevel;
        public final List<Milestone> milestones;

        public Resolved(ResourceLocation points, int cost, boolean repeatable, int maxLevel, int levelCost, int freeLevelEvery,
                        double bonusPerLevel, List<Milestone> milestones) {
            this.points = points;
            this.cost = cost;
            this.repeatable = repeatable;
            this.maxLevel = maxLevel;
            this.levelCost = levelCost;
            this.freeLevelEvery = freeLevelEvery;
            this.bonusPerLevel = bonusPerLevel;
            this.milestones = milestones;
        }

        public boolean isNative() {
            return PointTypes.NATIVE.equals(points);
        }

        /** True when this skill does not behave like a plain PST skill. */
        public boolean special() {
            return !isNative() || cost != 1;
        }
    }
}
