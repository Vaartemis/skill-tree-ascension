package dev.skillascension.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.skillascension.net.AscensionNet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Loads the three datapack folders on the server and mirrors them to clients. */
public final class AscensionData {
    private static final Gson GSON = new GsonBuilder().create();
    static final String POINT_TYPES = "skillascension/point_types";
    static final String NODES = "skillascension/nodes";
    static final String RANK_TIERS = "skillascension/rank_tiers";
    static final String TREE_ACCESS = "skillascension/tree_access";

    private static final Map<String, Map<ResourceLocation, JsonElement>> RAW = new HashMap<>();

    private AscensionData() {
    }

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        // order matters: point types before node rules (rules check that their point type exists)
        event.addListener(new Folder(POINT_TYPES, PointTypes::load));
        event.addListener(new Folder(RANK_TIERS, RankTiers::load));
        event.addListener(new Folder(NODES, NodeRules::load));
        event.addListener(new Folder(TREE_ACCESS, TreeAccess::load));
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) AscensionNet.sendData(event.getPlayer());
        else event.getPlayerList().getPlayers().forEach(AscensionNet::sendData);
        if (event.getPlayer() == null) dev.skillascension.points.TreeAccessEvents.resendAll(event.getPlayerList().getPlayers());
        if (event.getPlayer() == null) dev.skillascension.gates.GateEvents.resendAll(event.getPlayerList().getPlayers());
        else dev.skillascension.gates.GateEvents.update(event.getPlayer(), true);
    }

    public static synchronized void write(FriendlyByteBuf buf) {
        for (String folder : new String[]{POINT_TYPES, RANK_TIERS, NODES}) {
            Map<ResourceLocation, JsonElement> files = RAW.getOrDefault(folder, Map.of());
            buf.writeVarInt(files.size());
            files.forEach((id, json) -> {
                buf.writeResourceLocation(id);
                buf.writeUtf(GSON.toJson(json), 1 << 20);
            });
        }
    }

    public static Map<String, Map<ResourceLocation, JsonElement>> read(FriendlyByteBuf buf) {
        Map<String, Map<ResourceLocation, JsonElement>> out = new HashMap<>();
        for (String folder : new String[]{POINT_TYPES, RANK_TIERS, NODES}) {
            int n = buf.readVarInt();
            Map<ResourceLocation, JsonElement> files = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) files.put(buf.readResourceLocation(), JsonParser.parseString(buf.readUtf(1 << 20)));
            out.put(folder, files);
        }
        return out;
    }

    /** Client side: apply what the server sent (skipped on the integrated server, which already has it). */
    public static void apply(Map<String, Map<ResourceLocation, JsonElement>> data) {
        PointTypes.load(data.getOrDefault(POINT_TYPES, Map.of()));
        RankTiers.load(data.getOrDefault(RANK_TIERS, Map.of()));
        NodeRules.load(data.getOrDefault(NODES, Map.of()));
    }

    private static final class Folder extends SimpleJsonResourceReloadListener {
        private final String folder;
        private final Consumer<Map<ResourceLocation, JsonElement>> sink;

        Folder(String folder, Consumer<Map<ResourceLocation, JsonElement>> sink) {
            super(GSON, folder);
            this.folder = folder;
            this.sink = sink;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            synchronized (AscensionData.class) {
                RAW.put(folder, Map.copyOf(files));
            }
            sink.accept(files);
        }
    }
}
