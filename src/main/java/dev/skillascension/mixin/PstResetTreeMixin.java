package dev.skillascension.mixin;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.points.PointWallet;
import dev.skillascension.pst.RepeatableSkills;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PST refunds 1 skill point per learned skill on a tree reset. Skills that cost another point type or more than one point
 * are corrected here, and extra levels of repeatable skills are refunded too.
 */
@Pseudo
@Mixin(targets = "daripher.skilltree.capability.skill.PlayerSkills", remap = false)
public abstract class PstResetTreeMixin {
    @Inject(method = "resetTree", at = @At("HEAD"), remap = false, require = 0)
    private void skillascension$refundCosts(ServerPlayer player, CallbackInfo ci) {
        IPlayerSkills self = (IPlayerSkills) (Object) this;
        for (PassiveSkill skill : self.getPlayerSkills()) {
            if (skill == null) continue;
            NodeRule.Resolved rule = NodeRules.of(skill);
            if (!rule.special()) continue;
            if (rule.isNative()) {
                self.grantSkillPoints(rule.cost - 1);
            } else {
                self.setSkillPoints(self.getSkillPoints() - 1);
                PointWallet.refund(player, self, skill, rule.cost);
            }
        }
    }

    @Inject(method = "resetTree", at = @At("RETURN"), remap = false, require = 0)
    private void skillascension$refundLevels(ServerPlayer player, CallbackInfo ci) {
        RepeatableSkills.onTreeReset(player, (IPlayerSkills) (Object) this);
    }
}
