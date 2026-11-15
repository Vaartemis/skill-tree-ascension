package dev.skillascension.mixin;

import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.client.ClientGates;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Client: a skill whose requirements are not met (or whose rival choice is taken) cannot be picked. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.SkillTreeWidgets", remap = false)
public abstract class PstGateMixin {
    @Shadow(remap = false) @Final private List<ResourceLocation> learnedSkills;

    @Inject(method = "canLearnSkill", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void skillascension$gates(PassiveSkill skill, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        List<ResourceLocation> taken = new ArrayList<>(learnedSkills);
        taken.addAll(((SkillTreeWidgets) (Object) this).newlyLearnedSkills);
        if (!ClientGates.open(skill, taken)) cir.setReturnValue(false);
    }
}
