package dev.skillascension.net;

import com.google.gson.JsonElement;
import dev.skillascension.client.PstClientPoints;
import dev.skillascension.client.PstClientRanks;
import dev.skillascension.client.PstCooldownHud;
import dev.skillascension.data.AscensionData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

/** Only loaded on the client. */
final class ClientHandlers {
    private ClientHandlers() {
    }

    static void ranks(Map<ResourceLocation, Integer> ranks) {
        PstClientRanks.set(ranks);
    }

    static void points(Map<ResourceLocation, Integer> points) {
        PstClientPoints.set(points);
    }

    static void cooldown(String kind, int ticks) {
        PstCooldownHud.start(kind, ticks);
    }

    static void data(Map<String, Map<ResourceLocation, JsonElement>> data) {
        // the integrated server already loaded the same files into the shared registries
        if (Minecraft.getInstance().getSingleplayerServer() == null) AscensionData.apply(data);
    }
}
