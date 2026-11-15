package dev.skillascension.pst;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.config.ServerConfig;
import daripher.skilltree.data.reloader.SkillTreesReloader;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.exp.ExpHelper;
import daripher.skilltree.network.NetworkDispatcher;
import daripher.skilltree.network.message.SyncPlayerSkillsMessage;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.SkillAscension;
import dev.skillascension.common.Limits;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.points.PointSources;
import dev.skillascension.points.PointWallet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;

/**
 * Server side of learning skills and buying skill points. PST handles both messages on the network thread;
 * these run on the server thread instead and know about point types, costs and repeatable levels.
 */
public final class PstServerSkills {
    private PstServerSkills() {
    }

    public static void learn(ServerPlayer player, ResourceLocation skillId) {
        if (player.isRemoved()) return;
        IPlayerSkills capability = PlayerSkillsProvider.get(player);
        PassiveSkill skill = SkillsReloader.getSkillById(skillId);
        if (skill != null && dev.skillascension.data.TreeAccess.isLocked(player, NodeRules.treeOf(skillId))) {
            SkillAscension.LOG.info("{} tried to learn {} in a tree they have no access to; refused.", player.getGameProfile().getName(), skillId);
            skill = null;
        }
        if (skill != null) {
            NodeRule.Resolved rule = NodeRules.of(skill);
            if (rule.repeatable && RepeatableSkills.hasLearned(capability, skill)) {
                RepeatableSkills.rankUp(player, capability, skill);
            } else {
                net.minecraft.network.chat.MutableComponent gate = dev.skillascension.gates.SkillGates.blocked(player, capability, skill);
                String blockedTag = gate != null ? null : limitReached(player, capability, skill);
                if (gate != null) {
                    player.displayClientMessage(gate.withStyle(net.minecraft.ChatFormatting.RED), true);
                } else
                if (blockedTag != null) {
                    SkillAscension.LOG.info("{} tried to learn {} past the \"{}\" limit of its skill tree; refused.",
                            player.getGameProfile().getName(), skillId, blockedTag);
                } else if (rule.special()) {
                    if (PointWallet.available(player, capability, skill) >= rule.cost && capability.grantSkill(skill)) {
                        PointWallet.pay(player, capability, skill, rule.cost);
                        dev.skillascension.gates.SkillGates.pay(player, skill);
                        skill.learn(player, true);
                    }
                } else if (capability.learnSkill(skill)) {
                    dev.skillascension.gates.SkillGates.pay(player, skill);
                    skill.learn(player, true);
                }
            }
        }
        if (skill != null) {
            dev.skillascension.gates.GateEvents.update(player, false);
            if (RepeatableSkills.isRepeatable(skill)) RepeatableSkills.applyAttributes(player);
        }
        sync(player);
        PointSources.update(player);
    }

    public static void gainSkillPoint(ServerPlayer player) {
        if (player.isRemoved()) return;
        IPlayerSkills capability = PlayerSkillsProvider.get(player);
        int level = PointSources.earned(player, capability);
        if (level >= ServerConfig.max_skill_points) return;
        int cost = ServerConfig.getSkillPointCost(level);
        if (ExpHelper.getPlayerExp(player) >= cost) {
            player.giveExperiencePoints(-cost);
            capability.grantSkillPoints(1);
            sync(player);
            PointSources.update(player);
        }
    }

    /** Gives (or takes, if negative) PST skill points. */
    public static void grantNative(ServerPlayer player, int amount) {
        IPlayerSkills capability = PlayerSkillsProvider.get(player);
        capability.setSkillPoints(Math.max(0, capability.getSkillPoints() + amount));
        sync(player);
    }

    private static String limitReached(ServerPlayer player, IPlayerSkills capability, PassiveSkill skill) {
        if (skill.getTags().isEmpty()) return null;
        boolean solo = player.server.isSingleplayer() && !player.server.isPublished();
        for (PassiveSkillTree tree : SkillTreesReloader.getSkillTrees().values()) {
            if (!tree.getSkillIds().contains(skill.getId())) continue;
            for (Map.Entry<String, Integer> limit : tree.getSkillLimitations().entrySet()) {
                if (limit.getValue() == null || limit.getValue() <= 0 || !skill.getTags().contains(limit.getKey())) continue;
                int max = Limits.limit(limit.getKey(), limit.getValue(), solo);
                long learned = capability.getPlayerSkills().stream().filter(s -> s != null && s.getTags().contains(limit.getKey())).count();
                if (learned >= max) return limit.getKey();
            }
        }
        return null;
    }

    public static void sync(ServerPlayer player) {
        NetworkDispatcher.network_channel.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerSkillsMessage(player));
    }
}
