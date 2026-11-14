package dev.skillascension.mixin;

import dev.skillascension.client.PstClientRanks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.ProgressBar"},
   remap = false
)
public abstract class PstProgressBarRanksMixin {
   @ModifyVariable(
      method = {"renderCurrentLevel", "renderNextLevel"},
      at = @At(
         value = "STORE",
         ordinal = 0
      ),
      ordinal = 0,
      remap = false,
      require = 0
   )
   private int skillascension$levelWithRanks(int var1) {
      return var1 + PstClientRanks.levelDelta();
   }
}
