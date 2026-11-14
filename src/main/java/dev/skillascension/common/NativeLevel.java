package dev.skillascension.common;

import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.function.ToIntFunction;

/**
 * PST prices the next skill point by the player's "level" = learned skills + unspent points, i.e. the
 * native points they ever earned. With custom costs, other point types and repeatable levels that sum is wrong,
 * so both sides use this instead: unspent points + native points actually spent on skills and levels.
 */
public final class NativeLevel {
    private NativeLevel() {
    }

    public static int spent(Collection<PassiveSkill> learned, ToIntFunction<ResourceLocation> rankOf) {
        int spent = 0;
        for (PassiveSkill skill : learned) {
            if (skill == null) continue;
            NodeRule.Resolved rule = NodeRules.of(skill);
            if (!rule.isNative()) continue;
            spent += rule.cost + Math.max(0, rankOf.applyAsInt(skill.getId())) * rule.levelCost;
        }
        return spent;
    }

    /** What to add to PST's own level (learned skills + points) to get the real native level. */
    public static int delta(Collection<PassiveSkill> learned, ToIntFunction<ResourceLocation> rankOf) {
        int count = 0;
        for (PassiveSkill skill : learned) if (skill != null) count++;
        return spent(learned, rankOf) - count;
    }
}
