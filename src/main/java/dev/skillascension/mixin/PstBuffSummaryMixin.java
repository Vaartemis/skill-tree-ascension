package dev.skillascension.mixin;

import dev.skillascension.client.PstBuffSummary;
import daripher.skilltree.client.widget.skill.SkillButton;
import daripher.skilltree.client.widget.skill.SkillButtons;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.client.widget.SkillTreeWidgets"},
   remap = false
)
public abstract class PstBuffSummaryMixin {
   @Shadow(
      remap = false
   )
   @Final
   private List<ResourceLocation> learnedSkills;
   @Shadow(
      remap = false
   )
   @Final
   private SkillButtons skills;

   @Inject(
      method = {"getMergedSkillBonusesTooltips"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$readableSummary(CallbackInfoReturnable<List<Component>> var1) {
      try {
         ArrayList var2 = new ArrayList();

         for (ResourceLocation var4 : new LinkedHashSet<>(this.learnedSkills)) {
            SkillButton var5 = this.skills.getWidgetById(var4);
            if (var5 != null) {
               var2.add(var5.skill);
            }
         }

         var1.setReturnValue(PstBuffSummary.build(var2));
      } catch (Throwable var6) {
      }
   }
}
