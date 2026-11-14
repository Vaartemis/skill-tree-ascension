package dev.skillascension.mixin;

import daripher.skilltree.skill.PassiveSkillTree;
import dev.skillascension.AscensionConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Tree order on PST's selection screen follows the treeOrder client config. */
@Pseudo
@Mixin(targets = "daripher.skilltree.client.screen.SkillTreeSelectionScreen", remap = false)
public abstract class PstTreeOrderMixin {
    @Inject(method = "getNonEmptySkillTrees", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void skillascension$order(CallbackInfoReturnable<List<PassiveSkillTree>> cir) {
        List<PassiveSkillTree> trees = new ArrayList<>(cir.getReturnValue());
        trees.removeIf(t -> dev.skillascension.common.LockedTrees.isLocked(t.getId()));
        trees.sort(Comparator.comparingInt((PassiveSkillTree t) -> AscensionConfig.order(t.getId())).thenComparing(t -> t.getId().toString()));
        cir.setReturnValue(trees);
    }
}
