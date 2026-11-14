package dev.skillascension.mixin;

import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.client.PstClientRanks;
import dev.skillascension.client.PstRankQueue;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.PointTypes;
import dev.skillascension.data.RankTiers;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Tooltip of repeatable skills: level (and cap or tier), free levels, level cost and queued levels. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.skill.SkillButton", remap = false)
public abstract class PstRepeatableTooltipMixin {
    @Inject(method = "getSkillTooltip", at = @At("RETURN"), remap = false, require = 0)
    private void skillascension$showRank(PassiveSkillTree tree, CallbackInfoReturnable<List<MutableComponent>> cir) {
        SkillButton button = (SkillButton) (Object) this;
        PassiveSkill skill = button.skill;
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (!rule.repeatable) return;
        List<MutableComponent> tooltip = cir.getReturnValue();
        int level = button.skillLearned ? 1 + PstClientRanks.rank(skill.getId()) : 0;
        int max = rule.maxLevel;
        int at = Math.min(1, tooltip.size());
        MutableComponent line = (max > 0
                ? Component.translatable("skillascension.skill_rank_max", level, max)
                : Component.translatable("skillascension.skill_rank", level)).withStyle(ChatFormatting.GOLD);
        if (max <= 0 && level > 0) {
            RankTiers.Tier tier = RankTiers.at(level);
            if (tier != null) {
                line.append(Component.literal("  "));
                line.append(tier.name().copy().withStyle(Style.EMPTY.withColor(tier.color() & 0xFFFFFF).withBold(true)));
            }
        }
        tooltip.add(at++, line);
        if (max <= 0 && rule.freeLevelEvery > 0) {
            int free = RepeatableSkills.freeLevels(skill, level);
            int next = (level / rule.freeLevelEvery + 1) * rule.freeLevelEvery;
            tooltip.add(at++, Component.translatable("skillascension.skill_rank.milestone", free, rule.freeLevelEvery, next)
                    .withStyle(Style.EMPTY.withColor(0x9AA7FF)));
        }
        if (rule.levelCost != 1 || !rule.isNative()) {
            tooltip.add(at++, Component.translatable("skillascension.skill_rank.level_cost", rule.levelCost, PointTypes.get(rule.points).name)
                    .withStyle(ChatFormatting.GOLD));
        }
        if (button.skillLearned) {
            int queued = PstRankQueue.queued(skill.getId());
            if (queued > 0) {
                tooltip.add(at++, Component.translatable("skillascension.skill_rank_queued", queued).withStyle(ChatFormatting.YELLOW));
                tooltip.add(at++, Component.translatable("skillascension.skill_rank_total", level + queued).withStyle(ChatFormatting.GREEN));
            }
            tooltip.add(at, Component.translatable("skillascension.skill_rank_help").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
