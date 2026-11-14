package dev.skillascension.mixin;

import daripher.skilltree.client.widget.Button;
import daripher.skilltree.client.widget.Label;
import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.client.PstClientPoints;
import dev.skillascension.client.PstClientRanks;
import dev.skillascension.client.PstRankQueue;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Clicking a learned repeatable skill opens the  - / level / + / Max  panel; levels are queued and paid on Confirm. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.SkillTreeWidgets", remap = false)
public abstract class PstRepeatableClickMixin {
    @Shadow(remap = false) @Final private List<ResourceLocation> learnedSkills;

    @Inject(method = "init", at = @At("TAIL"), remap = false, require = 0)
    private void skillascension$panel(CallbackInfo ci) {
        SkillTreeWidgets self = (SkillTreeWidgets) (Object) this;
        if (PstRankQueue.widgets != self) PstRankQueue.selected = null;
        PstRankQueue.widgets = self;
        PstRankQueue.learned = learnedSkills;
        ResourceLocation id = PstRankQueue.selected;
        PassiveSkill skill = id == null ? null : SkillsReloader.getSkillById(id);
        if (skill == null || !learnedSkills.contains(id)) return;
        AbstractWidget widget = (AbstractWidget) (Object) this;
        int x = widget.getWidth() / 2 - 136;
        int y = widget.getHeight() - 40;
        Button minus = new Button(x, y, 18, 14, Component.literal("-"));
        minus.setPressFunc(b -> change(self, skill, Screen.hasShiftDown() ? -5 : -1));
        minus.setTooltip(Tooltip.create(Component.translatable("skillascension.skill_rank.minus")));
        self.addWidget(minus);
        Label label = new Label(x + 20, y, 196, 14, panelText(skill));
        label.setHasBackground(true);
        self.addWidget(label);
        Button plus = new Button(x + 218, y, 18, 14, Component.literal("+"));
        plus.setPressFunc(b -> change(self, skill, Screen.hasShiftDown() ? 5 : 1));
        plus.setTooltip(Tooltip.create(Component.translatable("skillascension.skill_rank.plus")));
        self.addWidget(plus);
        Button max = new Button(x + 238, y, 34, 14, Component.translatable("skillascension.skill_rank.max_button"));
        max.setPressFunc(b -> change(self, skill, 100000));
        max.setTooltip(Tooltip.create(Component.translatable("skillascension.skill_rank.max_tip")));
        self.addWidget(max);
    }

    private static MutableComponent panelText(PassiveSkill skill) {
        int level = 1 + PstClientRanks.rank(skill.getId());
        int queued = PstRankQueue.queued(skill.getId());
        MutableComponent name = Component.translatable("skill." + skill.getId().getNamespace() + "." + skill.getId().getPath() + ".name");
        return queued > 0
                ? Component.translatable("skillascension.skill_rank.panel_queued", name, level, level + queued, queued)
                : Component.translatable("skillascension.skill_rank.panel", name, level);
    }

    private static void change(SkillTreeWidgets self, PassiveSkill skill, int delta) {
        boolean changed = false;
        if (delta < 0) {
            for (int i = 0; i < -delta && PstClientPoints.unqueue(self, skill); i++) changed = true;
        } else {
            int queued = PstRankQueue.queued(skill.getId());
            int rank = PstClientRanks.rank(skill.getId());
            for (int i = 0; i < delta && PstClientPoints.canAffordLevel(self, skill) && !RepeatableSkills.atCap(skill, rank + queued); i++) {
                PstClientPoints.queue(self, skill, true);
                queued++;
                changed = true;
            }
        }
        if (changed) self.rebuildWidgets();
    }

    @Inject(method = "skillButtonPressed", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void skillascension$select(SkillButton button, CallbackInfo ci) {
        PassiveSkill skill = button.skill;
        if (!RepeatableSkills.isRepeatable(skill) || !learnedSkills.contains(skill.getId())) return;
        SkillTreeWidgets self = (SkillTreeWidgets) (Object) this;
        PstRankQueue.selected = skill.getId().equals(PstRankQueue.selected) && PstRankQueue.queued(skill.getId()) == 0 ? null : skill.getId();
        self.rebuildWidgets();
        ci.cancel();
    }
}
