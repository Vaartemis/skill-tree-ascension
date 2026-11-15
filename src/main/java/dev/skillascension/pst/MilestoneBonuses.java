package dev.skillascension.pst;

import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.bonus.SkillBonus;
import daripher.skilltree.skill.bonus.player.AttributeBonus;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Adds the one-time bonuses of reached milestones of learned repeatable skills to PST's bonus lists. */
public final class MilestoneBonuses {
    /** Conditional attribute bonuses are applied by PST's tick with the modifier's own id. PST's copy() gives a
     *  random id, so every copy handed out here is cached with a stable id (same key as the static path). */
    private static final Map<String, AttributeBonus> STABLE = new ConcurrentHashMap<>();
    private static Field modifierField;

    private MilestoneBonuses() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void add(Player player, Map<ResourceLocation, Integer> ranks, Class<?> type, List list) {
        if (player == null || !PlayerSkillsProvider.hasSkills(player)) return;
        for (PassiveSkill skill : PlayerSkillsProvider.get(player).getPlayerSkills()) {
            if (skill == null || !RepeatableSkills.isRepeatable(skill)) continue;
            List<NodeRule.Milestone> milestones = NodeRules.of(skill).milestones;
            if (milestones.isEmpty()) continue;
            int level = 1 + ranks.getOrDefault(skill.getId(), 0);
            for (int m = 0; m < milestones.size(); m++) {
                NodeRule.Milestone milestone = milestones.get(m);
                if (level < milestone.level) continue;
                List<SkillBonus<?>> bonuses = milestone.bonuses();
                for (int i = 0; i < bonuses.size(); i++) {
                    SkillBonus<?> bonus = bonuses.get(i);
                    if (!type.isInstance(bonus)) continue;
                    if (bonus instanceof AttributeBonus attribute) {
                        if (!attribute.isDynamic()) continue; // static ones are permanent modifiers (RepeatableSkills)
                        AttributeBonus stable = stable(attribute, milestoneKey(skill.getId(), m, i), 1.0);
                        if (stable != null) list.add(stable);
                    } else {
                        list.add(bonus.copy());
                    }
                }
            }
        }
    }

    static String milestoneKey(ResourceLocation skill, int milestone, int index) {
        return "skillascension:milestone/" + skill + "/" + milestone + "/" + index;
    }

    static String rankKey(ResourceLocation skill, int index) {
        return "skillascension:rank/" + skill + "/" + index;
    }

    static UUID id(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    /** A copy of a conditional attribute bonus with a stable modifier id and its amount times factor. */
    public static AttributeBonus stable(AttributeBonus source, String key, double factor) {
        AttributeModifier base = source.getModifier();
        if (base == null || source.getAttribute() == null) return null;
        double amount = base.getAmount() * factor;
        String cacheKey = key + "|" + amount + "|" + System.identityHashCode(source);
        AttributeBonus cached = STABLE.get(cacheKey);
        if (cached != null) return cached;
        try {
            if (modifierField == null) {
                Field f = AttributeBonus.class.getDeclaredField("modifier");
                f.setAccessible(true);
                modifierField = f;
            }
            AttributeBonus copy = source.copy();
            modifierField.set(copy, new AttributeModifier(id(key), base.getName(), amount, base.getOperation()));
            if (STABLE.size() > 8192) STABLE.clear(); // datapack reloads leave old entries behind
            STABLE.put(cacheKey, copy);
            return copy;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
