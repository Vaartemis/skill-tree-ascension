package dev.skillascension.mixin;

import dev.skillascension.client.pstui.AscensionUi;
import dev.skillascension.client.pstui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.Label", "daripher.skilltree.client.widget.TextField"},
   remap = false
)
public abstract class PstUiFieldStyleMixin {
   @Redirect(
      method = {"m_87963_"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;m_280218_(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
      ),
      remap = false,
      require = 0
   )
   private void skillascension$inset(GuiGraphics var1, ResourceLocation var2, int var3, int var4, int var5, int var6, int var7, int var8) {
      if (!AscensionUi.enabled()) {
         var1.blit(var2, var3, var4, var5, var6, var7, var8);
      } else {
         if (var5 == 0) {
            UiDraw.panel(var1, var3, var4, var7 * 2, var8, -804517614, -12831408);
         }
      }
   }
}
