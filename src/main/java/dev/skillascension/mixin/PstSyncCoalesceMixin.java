package dev.skillascension.mixin;

import dev.skillascension.client.PstScreenWatch;
import daripher.skilltree.client.screen.SkillTreeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.network.message.SyncPlayerSkillsMessage"},
   remap = false
)
public abstract class PstSyncCoalesceMixin {
   @Redirect(
      method = {"handlePacket"},
      at = @At(
         value = "INVOKE",
         target = "Ldaripher/skilltree/client/screen/SkillTreeScreen;m_7856_()V"
      ),
      remap = false,
      require = 0
   )
   private static void skillascension$oneRebuild(SkillTreeScreen var0) {
      PstScreenWatch.deferInit(var0);
   }
}
