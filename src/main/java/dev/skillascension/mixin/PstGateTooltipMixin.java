package dev.skillascension.mixin;

import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.client.ClientGates;
import dev.skillascension.client.PstRankQueue;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.Requirement;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Tooltip: requirements with a check or cross, offerings marked, and the other side of a choice. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.skill.SkillButton", remap = false)
public abstract class PstGateTooltipMixin {
    @Inject(method = "getSkillTooltip", at = @At("RETURN"), remap = false, require = 0)
    private void skillascension$requirements(PassiveSkillTree tree, CallbackInfoReturnable<List<MutableComponent>> cir) {
        SkillButton button = (SkillButton) (Object) this;
        PassiveSkill skill = button.skill;
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (!rule.gated() || button.skillLearned) return;
        List<MutableComponent> tooltip = cir.getReturnValue();
        int bits = ClientGates.bits(skill.getId());
        tooltip.add(Component.empty());
        if (!rule.requires.isEmpty()) {
            tooltip.add(Component.translatable("skillascension.requirement.header").withStyle(ChatFormatting.GOLD));
            for (int i = 0; i < rule.requires.size() && i < 30; i++) {
                Requirement r = rule.requires.get(i);
                boolean met = (bits & (1 << i)) != 0;
                MutableComponent line = Component.literal(met ? " ✔ " : " ✖ ").withStyle(Requirement.color(met));
                line.append(r.describe().withStyle(met ? ChatFormatting.GRAY : ChatFormatting.WHITE));
                if ("offer".equals(r.type)) line.append(Component.translatable("skillascension.requirement.consumed").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(line);
            }
        }
        if (rule.exclusive != null) {
            List<ResourceLocation> rivals = NodeRules.rivals(skill);
            if (!rivals.isEmpty()) {
                MutableComponent names = Component.empty();
                for (int i = 0; i < rivals.size(); i++) {
                    if (i > 0) names.append(Component.translatable("skillascension.requirement.or"));
                    PassiveSkill other = SkillsReloader.getSkillById(rivals.get(i));
                    ResourceLocation id = other == null ? rivals.get(i) : other.getId();
                    names.append(Component.translatable("skill." + id.getNamespace() + "." + id.getPath() + ".name"));
                }
                List<ResourceLocation> learned = PstRankQueue.learned;
                boolean taken = learned != null && rivals.stream().anyMatch(learned::contains);
                tooltip.add(Component.translatable(taken ? "skillascension.requirement.choice_taken" : "skillascension.requirement.choice", names)
                        .withStyle(taken ? ChatFormatting.RED : ChatFormatting.LIGHT_PURPLE));
            }
        }
    }
}
