package dev.skillascension.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.skillascension.SkillAscension;
import dev.skillascension.compat.OriginsCompat;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.skill.PassiveSkill;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Trees only some players can see and learn. Datapack files in
 * {@code data/<namespace>/skillascension/tree_access/<name>.json}:
 * <pre>{"trees": ["mypack:dragon"], "mode": "any", "requires": [{"type": "origin", "origin": "origins:elytrian"}]}</pre>
 * A tree named by several files must pass all of them. Locked trees are hidden from the player and cannot be learned.
 */
public final class TreeAccess {
    private static volatile List<Rule> rules = List.of();

    private TreeAccess() {
    }

    static void load(Map<ResourceLocation, JsonElement> files) {
        List<Rule> list = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                list.add(new Rule(json.getAsJsonObject()));
            } catch (Exception e) {
                SkillAscension.LOG.error("Could not read tree access {}: {}", id, e.toString());
            }
        });
        rules = List.copyOf(list);
    }

    public static boolean any() {
        return !rules.isEmpty();
    }

    /** Trees this player may not see or learn. */
    public static Set<ResourceLocation> locked(ServerPlayer player) {
        List<Rule> current = rules;
        if (current.isEmpty()) return Set.of();
        Set<ResourceLocation> out = new HashSet<>();
        for (Rule rule : current) {
            if (!rule.test(player)) out.addAll(rule.trees);
        }
        return out;
    }

    public static boolean isLocked(ServerPlayer player, ResourceLocation tree) {
        if (tree == null || rules.isEmpty()) return false;
        for (Rule rule : rules) {
            if (rule.trees.contains(tree) && !rule.test(player)) return true;
        }
        return false;
    }

    private static final class Rule {
        final List<ResourceLocation> trees;
        final boolean all;
        final List<Condition> requires = new ArrayList<>();

        Rule(JsonObject json) {
            this.trees = PointType.ids(json, json.has("trees") ? "trees" : "tree");
            this.all = "all".equalsIgnoreCase(GsonHelper.getAsString(json, "mode", "any"));
            if (json.has("requires")) {
                JsonElement r = json.get("requires");
                if (r.isJsonArray()) for (JsonElement e : r.getAsJsonArray()) requires.add(new Condition(e.getAsJsonObject()));
                else requires.add(new Condition(r.getAsJsonObject()));
            }
        }

        boolean test(ServerPlayer player) {
            if (requires.isEmpty()) return false; // no way in: the tree is closed to everyone
            for (Condition c : requires) {
                boolean ok = c.test(player);
                if (all && !ok) return false;
                if (!all && ok) return true;
            }
            return all;
        }
    }

    private static final class Condition {
        final String type;
        final ResourceLocation id;
        final ResourceLocation layer;
        final String tag;
        final int level;
        final boolean inverted;

        Condition(JsonObject json) {
            this.type = GsonHelper.getAsString(json, "type");
            String key = switch (type) {
                case "origin" -> "origin";
                case "power" -> "power";
                case "advancement" -> "advancement";
                case "skill" -> "skill";
                default -> null;
            };
            this.id = key != null ? new ResourceLocation(GsonHelper.getAsString(json, key)) : null;
            this.layer = json.has("layer") ? new ResourceLocation(GsonHelper.getAsString(json, "layer")) : null;
            this.tag = GsonHelper.getAsString(json, "tag", null);
            this.level = GsonHelper.getAsInt(json, "level", 2);
            this.inverted = GsonHelper.getAsBoolean(json, "inverted", false);
            if (!List.of("origin", "power", "advancement", "skill", "tag", "permission").contains(type)) {
                throw new IllegalArgumentException("unknown condition type " + type);
            }
        }

        boolean test(ServerPlayer player) {
            boolean result;
            switch (type) {
                case "origin" -> result = OriginsCompat.hasOrigin(player, id, layer);
                case "power" -> result = OriginsCompat.hasPower(player, id);
                case "advancement" -> {
                    Advancement adv = player.server.getAdvancements().getAdvancement(id);
                    result = adv != null && player.getAdvancements().getOrStartProgress(adv).isDone();
                }
                case "skill" -> {
                    result = false;
                    if (PlayerSkillsProvider.hasSkills(player)) {
                        for (PassiveSkill s : PlayerSkillsProvider.get(player).getPlayerSkills()) {
                            if (s != null && s.getId().equals(id)) {
                                result = true;
                                break;
                            }
                        }
                    }
                }
                case "tag" -> result = tag != null && player.getTags().contains(tag);
                case "permission" -> result = player.hasPermissions(level);
                default -> result = false;
            }
            return result != inverted;
        }
    }
}
