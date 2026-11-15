package dev.skillascension.client;

import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.gates.SkillGates;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** Client copy of requirement progress sent by the server. */
public final class ClientGates {
    private static volatile Map<ResourceLocation, Integer> status = Map.of();

    private ClientGates() {
    }

    public static void set(Map<ResourceLocation, Integer> map) {
        status = Map.copyOf(map);
    }

    public static int bits(ResourceLocation skill) {
        return status.getOrDefault(skill, 0);
    }

    /** Whether the skill may be picked, given the skills learned or queued on screen right now. */
    public static boolean open(PassiveSkill skill, List<ResourceLocation> learnedOrQueued) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (!rule.gated()) return true;
        if (rule.exclusive != null) {
            for (ResourceLocation rival : NodeRules.rivals(skill)) if (learnedOrQueued.contains(rival)) return false;
        }
        return SkillGates.open(rule, bits(skill.getId()) & ~SkillGates.EXCLUDED);
    }
}
