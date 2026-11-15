package dev.skillascension.gates;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.Requirement;
import dev.skillascension.data.TreeAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server side of skill requirements and exclusive choices. */
public final class SkillGates {
    /** Bit set in a status when another skill of the same exclusive group is learned. */
    public static final int EXCLUDED = 1 << 30;

    private SkillGates() {
    }

    /** Status bits: bit i = requirement i is met; {@link #EXCLUDED} = a rival choice is learned. */
    public static int status(ServerPlayer player, Set<ResourceLocation> learned, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        int bits = 0;
        List<Requirement> reqs = rule.requires;
        for (int i = 0; i < reqs.size() && i < 30; i++) {
            try {
                if (reqs.get(i).test(player)) bits |= 1 << i;
            } catch (Exception ignored) {
                // a broken requirement stays unmet
            }
        }
        if (rule.exclusive != null) {
            for (ResourceLocation rival : NodeRules.rivals(skill)) {
                if (learned.contains(rival)) {
                    bits |= EXCLUDED;
                    break;
                }
            }
        }
        return bits;
    }

    public static boolean open(NodeRule.Resolved rule, int bits) {
        if ((bits & EXCLUDED) != 0) return false;
        int n = Math.min(30, rule.requires.size());
        int all = n == 0 ? 0 : (n == 30 ? (1 << 30) - 1 : (1 << n) - 1);
        return (bits & all) == all;
    }

    public static Set<ResourceLocation> learned(ServerPlayer player) {
        Set<ResourceLocation> out = new HashSet<>();
        if (!PlayerSkillsProvider.hasSkills(player)) return out;
        for (PassiveSkill s : PlayerSkillsProvider.get(player).getPlayerSkills()) if (s != null) out.add(s.getId());
        return out;
    }

    /** Null when the player may learn the skill, otherwise the reason. */
    public static MutableComponent blocked(ServerPlayer player, IPlayerSkills capability, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (!rule.gated()) return null;
        int bits = status(player, learned(player), skill);
        if ((bits & EXCLUDED) != 0) return Component.translatable("skillascension.requirement.blocked_choice");
        for (int i = 0; i < rule.requires.size() && i < 30; i++) {
            if ((bits & (1 << i)) == 0) return Component.translatable("skillascension.requirement.blocked", rule.requires.get(i).describe());
        }
        return null;
    }

    /** Takes offerings once the skill is granted. */
    public static void pay(ServerPlayer player, PassiveSkill skill) {
        for (Requirement r : NodeRules.of(skill).requires) r.consume(player);
    }

    /** Status of every gated, not yet learned skill in trees this player can see. */
    public static Map<ResourceLocation, Integer> statuses(ServerPlayer player) {
        Map<ResourceLocation, Integer> out = new HashMap<>();
        Set<ResourceLocation> learned = learned(player);
        Set<ResourceLocation> lockedTrees = TreeAccess.locked(player);
        for (PassiveSkill skill : SkillsReloader.getSkills().values()) {
            if (skill == null || learned.contains(skill.getId())) continue;
            NodeRule.Resolved rule = NodeRules.of(skill);
            if (!rule.gated()) continue;
            ResourceLocation tree = NodeRules.treeOf(skill.getId());
            if (tree != null && lockedTrees.contains(tree)) continue;
            out.put(skill.getId(), status(player, learned, skill));
        }
        return out;
    }

    public static boolean anyGated() {
        for (PassiveSkill skill : SkillsReloader.getSkills().values()) {
            if (skill != null && NodeRules.of(skill).gated()) return true;
        }
        return false;
    }
}
