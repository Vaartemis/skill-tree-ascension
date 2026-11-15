package dev.skillascension.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A kind of point that skills can cost. Defined by datapacks in
 * {@code data/<namespace>/skillascension/point_types/<name>.json}; the file name is the id.
 * The PST skill points themselves are the "native" type ({@link PointTypes#NATIVE}).
 */
public final class PointType {
    public final ResourceLocation id;
    public final boolean nativePoints;
    public final Component name;
    public final int color;
    public final ResourceLocation iconTree;
    public final List<ResourceLocation> trees;
    public final String skillTag;
    public final int cost;
    public final ResourceLocation openTree;
    public final boolean showInHeader;
    public final int sort;
    public final boolean refundOnReset;
    public final int max;
    public final List<Source> sources;

    private PointType(ResourceLocation id, JsonObject json) {
        this.id = id;
        this.nativePoints = GsonHelper.getAsBoolean(json, "native", false);
        JsonElement nameJson = json.get("name");
        Component parsed = null;
        if (nameJson != null) {
            if (nameJson.isJsonPrimitive()) parsed = Component.literal(nameJson.getAsString());
            else parsed = Component.Serializer.fromJson(nameJson);
        }
        this.name = parsed != null ? parsed
                : Component.translatableWithFallback("skillascension.point_type." + id.getNamespace() + "." + id.getPath(), prettify(id.getPath()));
        this.color = parseColor(GsonHelper.getAsString(json, "color", nativePoints ? "#D8A84A" : "#7FB2FF"));
        this.iconTree = json.has("icon_tree") ? new ResourceLocation(GsonHelper.getAsString(json, "icon_tree")) : null;
        this.trees = ids(json, "trees");
        this.skillTag = GsonHelper.getAsString(json, "skill_tag", null);
        this.cost = Math.max(1, GsonHelper.getAsInt(json, "cost", 1));
        this.openTree = json.has("open_tree") ? new ResourceLocation(GsonHelper.getAsString(json, "open_tree"))
                : (trees.size() == 1 ? trees.get(0) : null);
        this.showInHeader = GsonHelper.getAsBoolean(json, "show_in_header", true);
        this.sort = GsonHelper.getAsInt(json, "sort", nativePoints ? 0 : 100);
        this.refundOnReset = GsonHelper.getAsBoolean(json, "refund_on_reset", true);
        this.max = Math.max(0, GsonHelper.getAsInt(json, "max", 0));
        List<Source> list = new ArrayList<>();
        if (json.has("sources")) {
            JsonArray array = GsonHelper.getAsJsonArray(json, "sources");
            for (int i = 0; i < array.size(); i++) list.add(new Source(i, array.get(i).getAsJsonObject()));
        }
        this.sources = Collections.unmodifiableList(list);
    }

    public static PointType parse(ResourceLocation id, JsonObject json) {
        return new PointType(id, json);
    }

    public static PointType nativeDefault() {
        JsonObject json = new JsonObject();
        json.addProperty("native", true);
        return new PointType(PointTypes.NATIVE, json);
    }

    static List<ResourceLocation> ids(JsonObject json, String key) {
        if (!json.has(key)) return List.of();
        List<ResourceLocation> out = new ArrayList<>();
        JsonElement e = json.get(key);
        if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) out.add(new ResourceLocation(x.getAsString()));
        else out.add(new ResourceLocation(e.getAsString()));
        return List.copyOf(out);
    }

    static int parseColor(String s) {
        String hex = s.startsWith("#") ? s.substring(1) : s;
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    static String prettify(String path) {
        String p = path.substring(path.lastIndexOf('/') + 1).replace('_', ' ');
        return p.isEmpty() ? p : Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    /** One way of earning this point type. */
    public static final class Source {
        public final int index;
        public final String type;
        public final int every;
        public final int amount;
        public final int max;
        /** Only what is earned past this many counts (e.g. Mastery points after the first 100 skill points). */
        public final int after;
        public final ResourceLocation advancement;

        Source(int index, JsonObject json) {
            this.index = index;
            this.type = GsonHelper.getAsString(json, "type");
            this.every = Math.max(1, GsonHelper.getAsInt(json, "every", 1));
            this.amount = GsonHelper.getAsInt(json, "amount", 1);
            this.max = Math.max(0, GsonHelper.getAsInt(json, "max", 0));
            this.after = Math.max(0, GsonHelper.getAsInt(json, "after", 0));
            this.advancement = json.has("advancement") ? new ResourceLocation(GsonHelper.getAsString(json, "advancement")) : null;
        }
    }
}
