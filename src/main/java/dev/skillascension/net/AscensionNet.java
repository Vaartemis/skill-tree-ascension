package dev.skillascension.net;

import com.google.gson.JsonElement;
import dev.skillascension.SkillAscension;
import dev.skillascension.data.AscensionData;
import dev.skillascension.points.PointWallet;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class AscensionNet {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SkillAscension.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    private AscensionNet() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, RanksMessage.class, RanksMessage::encode, RanksMessage::decode, RanksMessage::handle);
        CHANNEL.registerMessage(1, PointsMessage.class, PointsMessage::encode, PointsMessage::decode, PointsMessage::handle);
        CHANNEL.registerMessage(2, CooldownMessage.class, CooldownMessage::encode, CooldownMessage::decode, CooldownMessage::handle);
        CHANNEL.registerMessage(3, DataMessage.class, DataMessage::encode, DataMessage::decode, DataMessage::handle);
        CHANNEL.registerMessage(4, LockedMessage.class, LockedMessage::encode, LockedMessage::decode, LockedMessage::handle);
        CHANNEL.registerMessage(5, GatesMessage.class, GatesMessage::encode, GatesMessage::decode, GatesMessage::handle);
    }

    private static <T> void send(ServerPlayer player, T message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendRanks(ServerPlayer player) {
        send(player, new RanksMessage(RepeatableSkills.ranks(player)));
    }

    public static void sendPoints(ServerPlayer player) {
        send(player, new PointsMessage(PointWallet.all(player)));
    }

    public static void sendCooldown(ServerPlayer player, String kind, int ticks) {
        send(player, new CooldownMessage(kind, ticks));
    }

    public static void sendLocked(ServerPlayer player, java.util.Set<ResourceLocation> trees) {
        send(player, new LockedMessage(trees));
    }

    public record LockedMessage(java.util.Set<ResourceLocation> trees) {
        static void encode(LockedMessage m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.trees.size());
            for (ResourceLocation id : m.trees) buf.writeResourceLocation(id);
        }

        static LockedMessage decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            java.util.Set<ResourceLocation> set = new java.util.HashSet<>();
            for (int i = 0; i < n; i++) set.add(buf.readResourceLocation());
            return new LockedMessage(set);
        }

        static void handle(LockedMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> dev.skillascension.common.LockedTrees.set(m.trees));
        }
    }

    public static void sendGates(ServerPlayer player, Map<ResourceLocation, Integer> status) {
        send(player, new GatesMessage(status));
    }

    /** Requirement progress of gated skills (bit i = requirement i met). */
    public record GatesMessage(Map<ResourceLocation, Integer> status) {
        static void encode(GatesMessage m, FriendlyByteBuf buf) {
            buf.writeVarInt(m.status.size());
            m.status.forEach((id, bits) -> {
                buf.writeResourceLocation(id);
                buf.writeInt(bits);
            });
        }

        static GatesMessage decode(FriendlyByteBuf buf) {
            int n = buf.readVarInt();
            Map<ResourceLocation, Integer> map = new HashMap<>();
            for (int i = 0; i < n; i++) map.put(buf.readResourceLocation(), buf.readInt());
            return new GatesMessage(map);
        }

        static void handle(GatesMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> dev.skillascension.client.ClientGates.set(m.status));
        }
    }

    public static void sendData(ServerPlayer player) {
        send(player, new DataMessage(null));
    }

    private static void writeMap(FriendlyByteBuf buf, Map<ResourceLocation, Integer> map) {
        buf.writeVarInt(map.size());
        map.forEach((id, value) -> {
            buf.writeResourceLocation(id);
            buf.writeVarInt(value);
        });
    }

    private static Map<ResourceLocation, Integer> readMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) map.put(buf.readResourceLocation(), buf.readVarInt());
        return map;
    }

    private static void onClient(Supplier<NetworkEvent.Context> ctx, Runnable work) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> work));
        ctx.get().setPacketHandled(true);
    }

    public record RanksMessage(Map<ResourceLocation, Integer> ranks) {
        static void encode(RanksMessage m, FriendlyByteBuf buf) {
            writeMap(buf, m.ranks);
        }

        static RanksMessage decode(FriendlyByteBuf buf) {
            return new RanksMessage(readMap(buf));
        }

        static void handle(RanksMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> ClientHandlers.ranks(m.ranks));
        }
    }

    public record PointsMessage(Map<ResourceLocation, Integer> points) {
        static void encode(PointsMessage m, FriendlyByteBuf buf) {
            writeMap(buf, m.points);
        }

        static PointsMessage decode(FriendlyByteBuf buf) {
            return new PointsMessage(readMap(buf));
        }

        static void handle(PointsMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> ClientHandlers.points(m.points));
        }
    }

    public record CooldownMessage(String kind, int ticks) {
        static void encode(CooldownMessage m, FriendlyByteBuf buf) {
            buf.writeUtf(m.kind, 64);
            buf.writeVarInt(m.ticks);
        }

        static CooldownMessage decode(FriendlyByteBuf buf) {
            return new CooldownMessage(buf.readUtf(64), buf.readVarInt());
        }

        static void handle(CooldownMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> ClientHandlers.cooldown(m.kind, m.ticks));
        }
    }

    /** Datapack definitions; {@code data} is null when sending (written straight from the server copy). */
    public record DataMessage(Map<String, Map<ResourceLocation, JsonElement>> data) {
        static void encode(DataMessage m, FriendlyByteBuf buf) {
            AscensionData.write(buf);
        }

        static DataMessage decode(FriendlyByteBuf buf) {
            return new DataMessage(AscensionData.read(buf));
        }

        static void handle(DataMessage m, Supplier<NetworkEvent.Context> ctx) {
            onClient(ctx, () -> ClientHandlers.data(m.data));
        }
    }
}
