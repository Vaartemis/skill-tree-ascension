package dev.skillascension.data;

import com.google.gson.JsonElement;
import dev.skillascension.SkillAscension;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All point types known on this side (server: from datapacks, client: synced from the server). */
public final class PointTypes {
    /** The normal PST skill points. */
    public static final ResourceLocation NATIVE = new ResourceLocation(SkillAscension.MODID, "skill_points");

    private static volatile Map<ResourceLocation, PointType> types = Map.of(NATIVE, PointType.nativeDefault());
    private static volatile List<PointType> sorted = List.of(PointType.nativeDefault());

    private PointTypes() {
    }

    static void load(Map<ResourceLocation, JsonElement> files) {
        Map<ResourceLocation, PointType> map = new LinkedHashMap<>();
        map.put(NATIVE, PointType.nativeDefault());
        files.forEach((id, json) -> {
            try {
                PointType type = PointType.parse(id, json.getAsJsonObject());
                if (type.nativePoints && !id.equals(NATIVE)) {
                    SkillAscension.LOG.warn("Point type {} is marked native; only {} can be native, ignoring the flag", id, NATIVE);
                }
                map.put(id, id.equals(NATIVE) || !type.nativePoints ? type : PointType.parse(id, stripNative(json)));
            } catch (Exception e) {
                SkillAscension.LOG.error("Could not read point type {}: {}", id, e.toString());
            }
        });
        List<PointType> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparingInt((PointType t) -> t.sort).thenComparing(t -> t.id.toString()));
        types = Map.copyOf(map);
        sorted = List.copyOf(list);
        NodeRules.invalidate();
    }

    private static com.google.gson.JsonObject stripNative(JsonElement json) {
        com.google.gson.JsonObject copy = json.getAsJsonObject().deepCopy();
        copy.remove("native");
        return copy;
    }

    public static PointType get(ResourceLocation id) {
        PointType type = id == null ? null : types.get(id);
        return type != null ? type : types.get(NATIVE);
    }

    public static boolean exists(ResourceLocation id) {
        return id != null && types.containsKey(id);
    }

    /** Native first, then by "sort", then by id. */
    public static List<PointType> all() {
        return sorted;
    }

    public static List<PointType> extra() {
        List<PointType> out = new ArrayList<>();
        for (PointType t : sorted) if (!t.nativePoints) out.add(t);
        return out;
    }
}
