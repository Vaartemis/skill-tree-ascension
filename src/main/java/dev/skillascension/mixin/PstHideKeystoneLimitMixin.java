package dev.skillascension.mixin;

import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.common.Limits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.Map;

/** Limitation tags matching the hiddenLimitPattern config are still enforced but not listed in tooltips. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.widget.skill.SkillButton", remap = false)
public abstract class PstHideKeystoneLimitMixin {
    @Redirect(method = "addLimitationsTooltip", remap = false,
            at = @At(value = "INVOKE", target = "Ldaripher/skilltree/skill/PassiveSkillTree;getSkillLimitations()Ljava/util/Map;", remap = false), require = 0)
    private Map<String, Integer> skillascension$visibleLimits(PassiveSkillTree tree) {
        Map<String, Integer> shown = new HashMap<>();
        tree.getSkillLimitations().forEach((tag, limit) -> {
            if (!Limits.hidden(tag)) shown.put(tag, limit);
        });
        return shown;
    }
}
