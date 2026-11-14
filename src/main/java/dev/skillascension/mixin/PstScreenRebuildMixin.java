package dev.skillascension.mixin;

import dev.skillascension.client.PstScreenWatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.screen.SkillTreeScreen"},
   remap = false
)
public abstract class PstScreenRebuildMixin {
   @Shadow(
      remap = false
   )
   private boolean statsUpdated;

   @Inject(
      method = {"m_7819_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$statsOnce(CallbackInfo var1) {
      if (this.statsUpdated) {
         PstScreenWatch.statsSkipped();
         var1.cancel();
      }
   }

   @Inject(
      method = {"m_7856_"},
      at = {@At("HEAD")},
      remap = false,
      require = 0
   )
   private void skillascension$watchInit(CallbackInfo var1) {
      PstScreenWatch.init();
   }
}
