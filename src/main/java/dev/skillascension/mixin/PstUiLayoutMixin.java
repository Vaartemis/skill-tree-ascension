package dev.skillascension.mixin;

import dev.skillascension.client.pstui.AscensionUi;
import daripher.skilltree.client.widget.Button;
import daripher.skilltree.client.widget.Label;
import daripher.skilltree.client.widget.ProgressBar;
import daripher.skilltree.client.widget.ScrollableComponentList;
import daripher.skilltree.client.widget.SkillTreeWidgets;
import daripher.skilltree.skill.PassiveSkillTree;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.SkillTreeWidgets"},
   remap = false,
   priority = 1500
)
public abstract class PstUiLayoutMixin {
   @Shadow(
      remap = false
   )
   @Final
   private PassiveSkillTree skillTree;
   @Shadow(
      remap = false
   )
   private Button buyButton;
   @Shadow(
      remap = false
   )
   private Label pointsInfo;
   @Shadow(
      remap = false
   )
   private ProgressBar progressBar;
   @Shadow(
      remap = false
   )
   private ScrollableComponentList statsInfo;

   @Shadow(
      remap = false
   )
   private void buySkillPoint() {
   }

   @Inject(
      method = {"init"},
      at = {@At("TAIL")},
      remap = false,
      require = 0
   )
   private void skillascension$layout(CallbackInfo var1) {
      if (AscensionUi.enabled()) {
         AscensionUi.layout((SkillTreeWidgets)(Object)this, this.skillTree, this.progressBar, this.pointsInfo, this.buyButton, this.statsInfo, this::buySkillPoint);
      }
   }

   @Redirect(
      method = {"m_87963_"},
      at = @At(
         value = "INVOKE",
         target = "Ldaripher/skilltree/client/widget/ScrollableComponentList;m_252865_(I)V"
      ),
      remap = false,
      require = 0
   )
   private void skillascension$statsX(ScrollableComponentList var1, int var2) {
      var1.setX(AscensionUi.enabled() ? 40 : var2);
   }
}
