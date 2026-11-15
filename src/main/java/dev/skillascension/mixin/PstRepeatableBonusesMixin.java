package dev.skillascension.mixin;

import dev.skillascension.pst.RepeatableSkills;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.bonus.SkillBonus;
import daripher.skilltree.skill.bonus.player.AttributeBonus;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.skill.SkillBonusProvider"},
   remap = false
)
public abstract class PstRepeatableBonusesMixin {
   @Inject(
      method = {"getSkillTreeBonuses"},
      at = {@At("RETURN")},
      remap = false,
      require = 0
   )
   private static void skillascension$addRanks(Player player, Class<?> type, CallbackInfoReturnable<List> cir) {
      Map<ResourceLocation, Integer> ranks = RepeatableSkills.ranks(player);
      dev.skillascension.pst.MilestoneBonuses.add(player, ranks, type, (List)cir.getReturnValue());
      if (!ranks.isEmpty()) {
         List list = (List)cir.getReturnValue();
         ranks.forEach((id, rank) -> {
            PassiveSkill skill = SkillsReloader.getSkillById(id);
            if (skill != null) {
               double power = RepeatableSkills.power(skill, rank);

               List<SkillBonus<?>> bonuses = skill.getBonuses();
               for (int i = 0; i < bonuses.size(); i++) {
                  SkillBonus<?> bonus = bonuses.get(i);
                  if (!type.isInstance(bonus)) continue;
                  if (bonus instanceof AttributeBonus attribute) {
                     // Static attributes are permanent modifiers (RepeatableSkills.applyAttributes); conditional ones
                     // are ticked by PST, so they get a cached copy with a stable id that scales with the level.
                     if (attribute.isDynamic() && power > 0) {
                        AttributeBonus stable = dev.skillascension.pst.MilestoneBonuses.stable(attribute, "skillascension:rank/" + id + "/" + i, power);
                        if (stable != null) list.add(stable);
                     }
                  } else {
                     list.add(bonus.copy().multiply((double)power));
                  }
               }
            }
         });
      }
   }
}
