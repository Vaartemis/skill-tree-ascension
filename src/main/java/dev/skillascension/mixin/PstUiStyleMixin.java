package dev.skillascension.mixin;

import dev.skillascension.client.pstui.AscensionUi;
import dev.skillascension.client.pstui.UiDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.Button"},
   remap = false
)
public abstract class PstUiStyleMixin {
   @Inject(
      method = {"renderBackground"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$card(GuiGraphics var1, CallbackInfo var2) {
      if (AscensionUi.enabled()) {
         AbstractWidget var3 = (AbstractWidget)(Object)this;
         boolean var4 = var3.isActive();
         boolean var5 = var4 && var3.isHoveredOrFocused();
         int var6 = !var4 ? -12831408 : (var5 ? -11654 : -2578358);
         UiDraw.panel(var1, var3.getX(), var3.getY(), var3.getWidth(), var3.getHeight(), var5 ? -265937866 : -401206752, var6);
         var2.cancel();
      }
   }
}
