package dev.skillascension.mixin;

import dev.skillascension.client.pstui.AscensionUi;
import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.client.widget.skill.SkillButtons;
import daripher.skilltree.skill.PassiveSkillTree;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.screen.SkillTreeScreen"},
   remap = false
)
public abstract class PstUiScreenMixin {
   @Shadow(
      remap = false
   )
   @Final
   private PassiveSkillTree skillTree;
   @Shadow(
      remap = false
   )
   @Final
   private SkillButtons skillButtons;
   @Shadow(
      remap = false
   )
   @Final
   private SkillTreeWidgets skillTreeWidgets;

   @Redirect(
      method = {"m_88315_"},
      at = @At(
         value = "INVOKE",
         target = "Ldaripher/skilltree/client/widget/skill/SkillButtons;renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;FF)V"
      ),
      remap = false,
      require = 0
   )
   private void skillascension$layer(SkillButtons var1, GuiGraphics var2, float var3, float var4) {
      if (!AscensionUi.enabled()) {
         var1.renderTooltip(var2, var3, var4);
      } else {
         AscensionUi.render((Screen)(Object)this, var2, (int)var3, (int)var4, var1, this.skillTreeWidgets, this.skillTree);
      }
   }

   @Inject(
      method = {"m_6375_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$click(double var1, double var3, int var5, CallbackInfoReturnable<Boolean> var6) {
      if (AscensionUi.enabled() && AscensionUi.click((Screen)(Object)this, var1, var3, var5, this.skillTree)) {
         var6.setReturnValue(true);
      }
   }

   @Inject(
      method = {"m_6050_"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$scroll(double var1, double var3, double var5, CallbackInfoReturnable<Boolean> var7) {
      if (AscensionUi.enabled() && AscensionUi.scroll(var1, var3, var5)) {
         var7.setReturnValue(true);
      }
   }
}
