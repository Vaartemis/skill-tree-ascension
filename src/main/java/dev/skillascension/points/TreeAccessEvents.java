package dev.skillascension.points;

import dev.skillascension.data.TreeAccess;
import dev.skillascension.net.AscensionNet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Keeps each client's list of locked trees up to date (origin changes, advancements, tags...). */
public final class TreeAccessEvents {
    private static final Map<UUID, Set<ResourceLocation>> SENT = new HashMap<>();

    private TreeAccessEvents() {
    }

    public static void update(ServerPlayer player, boolean force) {
        Set<ResourceLocation> locked = TreeAccess.locked(player);
        Set<ResourceLocation> last = SENT.get(player.getUUID());
        if (!force && locked.equals(last)) return;
        SENT.put(player.getUUID(), locked);
        AscensionNet.sendLocked(player, locked);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player, true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player, true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.tickCount % 40 != 17) return;
        if (!TreeAccess.any() && !SENT.containsKey(player.getUUID())) return;
        update(player, false);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }

    /** After /reload: everyone gets a fresh list. */
    public static void resendAll(Iterable<ServerPlayer> players) {
        for (ServerPlayer p : players) update(p, true);
    }
}
