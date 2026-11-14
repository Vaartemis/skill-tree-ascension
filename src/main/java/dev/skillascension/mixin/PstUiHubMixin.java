package dev.skillascension.mixin;

import dev.skillascension.client.pstui.AscensionHub;
import dev.skillascension.client.pstui.AscensionUi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.screen.SkillTreeSelectionScreen"},
   remap = false
)
public abstract class PstUiHubMixin extends Screen {
   protected PstUiHubMixin(Component var1) {
      super(var1);
   }

   @Inject(
      method = {"m_7856_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$noButtons(CallbackInfo var1) {
      if (AscensionUi.hubEnabled()) {
         this.clearWidgets();
         var1.cancel();
      }
   }

   @Inject(
      method = {"m_88315_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$hub(GuiGraphics var1, int var2, int var3, float var4, CallbackInfo var5) {
      if (AscensionUi.hubEnabled()) {
         this.renderBackground(var1);
         AscensionHub.render(this, var1, var2, var3, var4);
         var5.cancel();
      }
   }

   public boolean mouseClicked(double var1, double var3, int var5) {
      return AscensionUi.hubEnabled() && AscensionHub.click(this, var1, var3, var5) ? true : super.mouseClicked(var1, var3, var5);
   }
}
