package dev.skillascension.mixin;

import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.client.PstPathHint;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.PointTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Tooltip lines: the cost when it is not "1 skill point", and how many points away a locked skill is. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.skill.SkillButton", remap = false)
public abstract class PstPathTooltipMixin {
    @Inject(method = "getSkillTooltip", at = @At("RETURN"), remap = false, require = 0)
    private void skillascension$pathHint(PassiveSkillTree tree, CallbackInfoReturnable<List<MutableComponent>> cir) {
        try {
            SkillButton button = (SkillButton) (Object) this;
            NodeRule.Resolved rule = NodeRules.of(button.skill);
            if (rule.special()) {
                cir.getReturnValue().add(Component.translatable("skillascension.points.cost", rule.cost, PointTypes.get(rule.points).name)
                        .withStyle(ChatFormatting.GOLD));
            }
            if (button.skillLearned || button.canLearn) return;
            int distance = PstPathHint.distance(tree, button.skill);
            if (distance <= 1) return;
            int free = PstPathHint.freePoints();
            cir.getReturnValue().add(Component.translatable("skillascension.skill_path", distance, free)
                    .withStyle(free >= distance ? ChatFormatting.GREEN : ChatFormatting.AQUA));
        } catch (Throwable ignored) {
        }
    }
}
