package dev.skillascension.mixin;

import daripher.skilltree.client.widget.skill.SkillButton;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.skill.SkillButton"},
   remap = false
)
public abstract class PstIconScaleMixin {
   @Unique
   private boolean skillascension$scaled;

   @Inject(
      method = {"renderIcon"},
      at = {@At("HEAD")},
      remap = false,
      require = 0
   )
   private void skillascension$scaleIcon(GuiGraphics graphics, CallbackInfo ci) {
      this.skillascension$scaled = false;
      SkillButton button = (SkillButton)(Object)this;
      float scale = 0.0F;

      for (String tag : button.skill.getTags()) {
         if (tag.startsWith("IconScale")) {
            try {
               scale = (float)Integer.parseInt(tag.substring(9)) / 100.0F;
            } catch (NumberFormatException var8) {
            }
         }
      }

      if (!(scale <= 0.0F) && scale != 1.0F) {
         scale = Math.max(0.25F, Math.min(3.0F, scale));
         float cx = (float)button.getWidth() / 2.0F;
         float cy = (float)button.getHeight() / 2.0F;
         graphics.pose().pushPose();
         graphics.pose().translate(cx, cy, 0.0F);
         graphics.pose().scale(scale, scale, 1.0F);
         graphics.pose().translate(-cx, -cy, 0.0F);
         this.skillascension$scaled = true;
      }
   }

   @Inject(
      method = {"renderIcon"},
      at = {@At("RETURN")},
      remap = false,
      require = 0
   )
   private void skillascension$restore(GuiGraphics graphics, CallbackInfo ci) {
      if (this.skillascension$scaled) {
         graphics.pose().popPose();
         this.skillascension$scaled = false;
      }
   }
}
