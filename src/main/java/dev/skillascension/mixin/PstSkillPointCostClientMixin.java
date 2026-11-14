package dev.skillascension.mixin;

import dev.skillascension.client.PstClientRanks;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.config.ServerConfig"},
   remap = false
)
public abstract class PstSkillPointCostClientMixin {
   @ModifyVariable(
      method = {"getSkillPointCost"},
      at = @At("HEAD"),
      argsOnly = true,
      remap = false,
      require = 0
   )
   private static int skillascension$countRanks(int level) {
      return EffectiveSide.get().isClient() ? level + PstClientRanks.levelDelta() : level;
   }
}
