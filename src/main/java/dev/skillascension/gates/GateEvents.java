package dev.skillascension.gates;

import dev.skillascension.net.AscensionNet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Keeps each client's view of requirement progress fresh (every 2 s, only when something changed). */
public final class GateEvents {
    private static final Map<UUID, Map<ResourceLocation, Integer>> SENT = new HashMap<>();
    private static Boolean anyGated;

    private GateEvents() {
    }

    public static void invalidate() {
        anyGated = null;
        SENT.clear();
    }

    public static void update(ServerPlayer player, boolean force) {
        if (anyGated == null) anyGated = SkillGates.anyGated();
        if (!anyGated && !SENT.containsKey(player.getUUID())) return;
        Map<ResourceLocation, Integer> now = SkillGates.statuses(player);
        if (!force && now.equals(SENT.get(player.getUUID()))) return;
        SENT.put(player.getUUID(), now);
        AscensionNet.sendGates(player, now);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player, true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 40 != 29) return;
        update(player, false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    public static void resendAll(Iterable<ServerPlayer> players) {
        anyGated = null;
        for (ServerPlayer p : players) update(p, true);
    }
}
