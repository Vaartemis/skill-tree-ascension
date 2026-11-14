package dev.skillascension.mixin;

import daripher.skilltree.client.widget.Label;
import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.client.PstClientPoints;
import dev.skillascension.client.pstui.AscensionUi;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Client: skills that cost other point types or more than 1 point are queued with their real cost. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.SkillTreeWidgets", remap = false)
public abstract class PstPointCostMixin {
    @Shadow(remap = false) @Final private List<ResourceLocation> learnedSkills;
    @Shadow(remap = false) @Final private PassiveSkillTree skillTree;

    @Inject(method = "skillButtonPressed", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void skillascension$payCost(SkillButton button, CallbackInfo ci) {
        PassiveSkill skill = button.skill;
        NodeRule.Resolved rule = NodeRules.of(skill);
        if (!rule.special()) return;
        if (rule.repeatable && learnedSkills.contains(skill.getId())) return; // handled by the level panel
        SkillTreeWidgets self = (SkillTreeWidgets) (Object) this;
        List<ResourceLocation> queue = self.newlyLearnedSkills;
        if (!queue.isEmpty() && queue.get(queue.size() - 1).equals(skill.getId())) {
            PstClientPoints.unqueue(self, skill);
        } else {
            if (!button.canLearn) {
                ci.cancel();
                return;
            }
            PstClientPoints.queue(self, skill, false);
        }
        self.rebuildWidgets();
        ci.cancel();
    }

    @Inject(method = "canLearnSkill", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void skillascension$affordable(PassiveSkill skill, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && !PstClientPoints.canAfford((SkillTreeWidgets) (Object) this, skill)) cir.setReturnValue(false);
    }

    @Redirect(method = "highlightSkills", at = @At(value = "FIELD", target = "Ldaripher/skilltree/client/widget/SkillTreeWidgets;skillPoints:I", opcode = 180), remap = false, require = 0)
    private int skillascension$anyPoints(SkillTreeWidgets self) {
        if (self.skillPoints > 0) return self.skillPoints;
        return PstClientPoints.anyExtraLeft(self) ? 1 : 0;
    }

    @Inject(method = "cancelLearnSkills", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void skillascension$cancel(CallbackInfo ci) {
        SkillTreeWidgets self = (SkillTreeWidgets) (Object) this;
        List<ResourceLocation> queue = List.copyOf(self.newlyLearnedSkills);
        for (int i = queue.size() - 1; i >= 0; i--) {
            PassiveSkill skill = SkillsReloader.getSkillById(queue.get(i));
            if (skill == null || !PstClientPoints.unqueue(self, skill)) self.newlyLearnedSkills.remove(queue.get(i));
        }
        self.newlyLearnedSkills.clear();
        self.rebuildWidgets();
        ci.cancel();
    }

    /** PST's own layout: a label with the extra point types this tree uses (our layout shows them in the header). */
    @Inject(method = "init", at = @At("TAIL"), remap = false, require = 0)
    private void skillascension$pointsLabel(CallbackInfo ci) {
        if (AscensionUi.enabled()) return;
        Set<ResourceLocation> used = new LinkedHashSet<>();
        for (ResourceLocation id : skillTree.getSkillIds()) {
            NodeRule.Resolved rule = NodeRules.of(SkillsReloader.getSkillById(id));
            if (!rule.isNative()) used.add(rule.points);
        }
        if (used.isEmpty()) return;
        SkillTreeWidgets self = (SkillTreeWidgets) (Object) this;
        MutableComponent text = Component.empty();
        boolean first = true;
        for (ResourceLocation typeId : used) {
            PointType type = PointTypes.get(typeId);
            if (!first) text.append("  ");
            first = false;
            text.append(Component.translatable("skillascension.points.label", type.name, PstClientPoints.left(self, typeId)));
        }
        int height = ((AbstractWidget) (Object) this).getHeight();
        Label label = new Label(4, height - 18, 220, 14, text);
        label.setHasBackground(true);
        self.addWidget(label);
    }
}
