package dev.skillascension.points;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import dev.skillascension.net.AscensionNet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/** Points of every non-native type a player holds. Stored in the player's persisted data, so it survives death. */
public final class PointWallet {
    static final String PERSISTED = "PlayerPersisted";
    private static final String KEY = "skillascension_points";

    private PointWallet() {
    }

    public static int get(Player player, ResourceLocation type) {
        return player.getPersistentData().getCompound(PERSISTED).getCompound(KEY).getInt(type.toString());
    }

    public static Map<ResourceLocation, Integer> all(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(PERSISTED).getCompound(KEY);
        Map<ResourceLocation, Integer> out = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) out.put(id, tag.getInt(key));
        }
        return out;
    }

    public static void set(ServerPlayer player, ResourceLocation type, int amount) {
        PointType def = PointTypes.get(type);
        int value = Math.max(0, amount);
        if (def.max > 0) value = Math.min(def.max, value);
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(PERSISTED);
        CompoundTag tag = persisted.getCompound(KEY);
        tag.putInt(type.toString(), value);
        persisted.put(KEY, tag);
        data.put(PERSISTED, persisted);
        AscensionNet.sendPoints(player);
    }

    public static void add(ServerPlayer player, ResourceLocation type, int amount) {
        if (PointTypes.NATIVE.equals(type)) {
            dev.skillascension.pst.PstServerSkills.grantNative(player, amount);
            return;
        }
        set(player, type, get(player, type) + amount);
    }

    /** Points of the skill's type the player can spend right now. */
    public static int available(ServerPlayer player, IPlayerSkills skills, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        return rule.isNative() ? skills.getSkillPoints() : get(player, rule.points);
    }

    public static void pay(ServerPlayer player, IPlayerSkills skills, PassiveSkill skill, int amount) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (rule.isNative()) skills.setSkillPoints(skills.getSkillPoints() - amount);
        else set(player, rule.points, get(player, rule.points) - amount);
    }

    public static void refund(ServerPlayer player, IPlayerSkills skills, PassiveSkill skill, int amount) {
        if (amount <= 0) return;
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (rule.isNative()) skills.grantSkillPoints(amount);
        else if (PointTypes.get(rule.points).refundOnReset) set(player, rule.points, get(player, rule.points) + amount);
    }
}
