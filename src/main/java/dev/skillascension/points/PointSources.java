package dev.skillascension.points;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Hands out points of every type from the "sources" of its definition.
 * <ul>
 *   <li>{@code skill_points_earned}: {@code amount} every {@code every} PST skill points the player ever earned (learned + unspent + extra levels), up to {@code max} in total.</li>
 *   <li>{@code xp_level}: {@code amount} every {@code every} experience levels, counting the highest level ever reached, up to {@code max}.</li>
 *   <li>{@code advancement}: {@code amount} once, when the player earns {@code advancement}.</li>
 * </ul>
 * Items, quests or anything else can give points with {@code /skilltree points add <player> <amount> <type>}.
 */
public final class PointSources {
    private static final String KEY = "skillascension_sources";

    private PointSources() {
    }

    /** Skill points the player has earned so far, spent or not. */
    public static int earned(ServerPlayer player, IPlayerSkills skills) {
        Map<ResourceLocation, Integer> ranks = RepeatableSkills.ranks(player);
        return skills.getSkillPoints() + dev.skillascension.common.NativeLevel.spent(skills.getPlayerSkills(), id -> ranks.getOrDefault(id, 0));
    }

    public static void update(ServerPlayer player) {
        if (player.isRemoved() || !PlayerSkillsProvider.hasSkills(player)) return;
        IPlayerSkills skills = PlayerSkillsProvider.get(player);
        CompoundTag data = data(player);
        int earned = earned(player, skills);
        int peak = Math.max(data.getInt("peak_level"), player.experienceLevel);
        data.putInt("peak_level", peak);
        boolean changed = false;
        for (PointType type : PointTypes.all()) {
            for (PointType.Source source : type.sources) {
                int target = switch (source.type) {
                    case "skill_points_earned" -> earned / source.every * source.amount;
                    case "xp_level" -> peak / source.every * source.amount;
                    default -> -1;
                };
                if (target < 0) continue;
                if (source.max > 0) target = Math.min(target, source.max);
                String key = type.id + "#" + source.index;
                int granted = data.getInt(key);
                if (target > granted) {
                    data.putInt(key, target);
                    give(player, type, target - granted);
                    changed = true;
                }
            }
        }
        if (changed || !data.isEmpty()) save(player, data);
    }

    private static void give(ServerPlayer player, PointType type, int amount) {
        PointWallet.add(player, type.id, amount);
        player.displayClientMessage(Component.translatable("skillascension.points.gained", amount, type.name).withStyle(ChatFormatting.AQUA), true);
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Advancement advancement = event.getAdvancement();
        CompoundTag data = data(player);
        for (PointType type : PointTypes.all()) {
            for (PointType.Source source : type.sources) {
                if (!"advancement".equals(source.type) || !advancement.getId().equals(source.advancement)) continue;
                String key = type.id + "#" + source.index;
                if (data.getBoolean(key)) continue;
                data.putBoolean(key, true);
                give(player, type, source.amount);
            }
        }
        save(player, data);
    }

    @SubscribeEvent
    public static void onLevel(PlayerXpEvent.LevelChange event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevels() > 0) {
            player.server.execute(() -> update(player));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    private static CompoundTag data(ServerPlayer player) {
        return player.getPersistentData().getCompound(PointWallet.PERSISTED).getCompound(KEY);
    }

    private static void save(ServerPlayer player, CompoundTag tag) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(PointWallet.PERSISTED);
        persisted.put(KEY, tag);
        data.put(PointWallet.PERSISTED, persisted);
    }
}
