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
    final List<Requirement> requires;
    final String exclusive;

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
        this.requires = Requirement.list(json);
        this.exclusive = GsonHelper.getAsString(json, "exclusive", null);
    }

    /** Something that happens once when a repeatable skill reaches a level. */
    public static final class Milestone {
        public final int level;
        public final List<String> commands;
        public final Component message;
        private final JsonArray bonusJson;
        private volatile List<daripher.skilltree.skill.bonus.SkillBonus<?>> bonuses;

        Milestone(JsonObject json) {
            this.bonusJson = json.has("bonuses") ? GsonHelper.getAsJsonArray(json, "bonuses") : new JsonArray();
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

        /** PST bonuses this milestone adds once (not multiplied by level) while the skill is at least at {@link #level}. */
        public List<daripher.skilltree.skill.bonus.SkillBonus<?>> bonuses() {
            List<daripher.skilltree.skill.bonus.SkillBonus<?>> list = bonuses;
            if (list != null) return list;
            List<daripher.skilltree.skill.bonus.SkillBonus<?>> out = new ArrayList<>();
            for (JsonElement e : bonusJson) {
                try {
                    JsonObject o = e.getAsJsonObject();
                    ResourceLocation type = new ResourceLocation(GsonHelper.getAsString(o, "type"));
                    daripher.skilltree.skill.bonus.SkillBonus.Serializer serializer =
                            daripher.skilltree.init.PSTRegistries.SKILL_BONUSES.get().getValue(type);
                    if (serializer == null) {
                        dev.skillascension.SkillAscension.LOG.error("Unknown bonus type {} in a milestone", type);
                        continue;
                    }
                    out.add(serializer.deserialize(o));
                } catch (Exception ex) {
                    dev.skillascension.SkillAscension.LOG.error("Could not read a milestone bonus: {}", ex.toString());
                }
            }
            bonuses = List.copyOf(out);
            return bonuses;
        }
    }

    /** The final rule for one skill after merging everything that matches it. */
    public static final class Resolved {
        public static final Resolved DEFAULT = new Resolved(PointTypes.NATIVE, 1, false, 0, 1, 10, 1.0, List.of(), List.of(), null);

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
        /** Must all pass before the skill is first learned (levels of a repeatable skill never need them). */
        public final List<Requirement> requires;
        /** Skills sharing this name exclude each other: learning one closes the others until the tree is reset. */
        public final String exclusive;

        public Resolved(ResourceLocation points, int cost, boolean repeatable, int maxLevel, int levelCost, int freeLevelEvery,
                        double bonusPerLevel, List<Milestone> milestones, List<Requirement> requires, String exclusive) {
            this.points = points;
            this.cost = cost;
            this.repeatable = repeatable;
            this.maxLevel = maxLevel;
            this.levelCost = levelCost;
            this.freeLevelEvery = freeLevelEvery;
            this.bonusPerLevel = bonusPerLevel;
            this.milestones = milestones;
            this.requires = requires;
            this.exclusive = exclusive;
        }

        public boolean isNative() {
            return PointTypes.NATIVE.equals(points);
        }

        /** True when this skill does not behave like a plain PST skill. */
        public boolean special() {
            return !isNative() || cost != 1;
        }

        public boolean gated() {
            return !requires.isEmpty() || exclusive != null;
        }
    }
}
