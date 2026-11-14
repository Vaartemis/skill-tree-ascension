package dev.skillascension.mixin;

import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.SkillTreeWidgets"},
   remap = false
)
public abstract class PstScreenCapabilityGuardMixin {
   @Shadow(
      remap = false
   )
   @Final
   private LocalPlayer player;

   @Inject(
      method = {"updateBuyPointButton"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$skipWithoutSkillCapability(CallbackInfo ci) {
      if (this.player == null || !PlayerSkillsProvider.hasSkills(this.player)) {
         ci.cancel();
      }
   }
}
