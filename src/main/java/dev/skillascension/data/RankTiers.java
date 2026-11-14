package dev.skillascension.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.skillascension.SkillAscension;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Names shown next to the level of an unlimited repeatable skill (Bronze, Silver...). Defined in
 * {@code data/<namespace>/skillascension/rank_tiers/<name>.json} as {@code {"tiers": [{"level": 10, "name": "Silver", "color": "#D8DCE3"}]}}.
 * Files are read in id order and the last one wins, so a pack replaces the default by shipping
 * {@code data/skillascension/skillascension/rank_tiers/default.json}, or adds its own file with {"replace": false} to extend it.
 */
public final class RankTiers {
    public record Tier(int level, Component name, int color) {
    }

    private static volatile List<Tier> tiers = List.of();

    private RankTiers() {
    }

    static void load(Map<ResourceLocation, JsonElement> files) {
        List<Tier> list = new ArrayList<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                JsonObject json = entry.getValue().getAsJsonObject();
                if (GsonHelper.getAsBoolean(json, "replace", true)) list.clear();
                for (JsonElement e : GsonHelper.getAsJsonArray(json, "tiers")) {
                    JsonObject t = e.getAsJsonObject();
                    JsonElement n = t.get("name");
                    Component name = n.isJsonPrimitive()
                            ? (n.getAsString().contains(".") && !n.getAsString().contains(" ") ? Component.translatable(n.getAsString()) : Component.literal(n.getAsString()))
                            : Component.Serializer.fromJson(n);
                    list.add(new Tier(GsonHelper.getAsInt(t, "level"), name, PointType.parseColor(GsonHelper.getAsString(t, "color", "#FFFFFF"))));
                }
            } catch (Exception ex) {
                SkillAscension.LOG.error("Could not read rank tiers {}: {}", entry.getKey(), ex.toString());
            }
        });
        list.sort(Comparator.comparingInt(Tier::level).reversed());
        tiers = List.copyOf(list);
    }

    /** The highest tier reached at this level, or null. */
    public static Tier at(int level) {
        for (Tier t : tiers) if (level >= t.level()) return t;
        return null;
    }
}
