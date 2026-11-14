package dev.skillascension.mixin;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.skill.bonus.player.InflictMobEffectBonus"},
   remap = false
)
public abstract class PstEffectStackingMixin {
   @Shadow
   private MobEffectInstance effectInstance;
   @Shadow
   private int maxStacks;
   @Unique
   private static final Logger skillascension$LOGGER = LogManager.getLogger("SkillAscension/PstStacking");
   @Unique
   private static final Map<LivingEntity, Map<MobEffect, Long>> skillascension$lastStack = new WeakHashMap<>();

   @Inject(
      method = {"getEffectInstanceAfterStacking"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$stackProperly(LivingEntity target, MobEffect effect, MobEffectInstance baseInstance, CallbackInfoReturnable<MobEffectInstance> cir) {
      MobEffectInstance active = target.getEffect(effect);
      if (active == null) {
         cir.setReturnValue(baseInstance);
      } else {
         int cap = Math.max(0, this.maxStacks - 1);
         int amplifier = Math.min(active.getAmplifier(), cap);
         long now = target.level().getGameTime();
         Map<MobEffect, Long> perEffect = skillascension$lastStack.computeIfAbsent(target, t -> new IdentityHashMap<>());
         Long last = perEffect.get(effect);
         if (last == null || now - last >= dev.skillascension.AscensionConfig.EFFECT_STACK_COOLDOWN.get() || now < last) {
            int added = this.effectInstance.getAmplifier() + 1;
            int stacked = (int)Math.min((long)active.getAmplifier() + (long)added, (long)cap);
            if (stacked > amplifier) {
               amplifier = stacked;
               perEffect.put(effect, now);
            }
         }

         if (skillascension$LOGGER.isDebugEnabled()) {
            skillascension$LOGGER.debug(
               "PST stack {} on {}: level {} -> {} (cap {}).",
               effect.getDescriptionId(),
               target.getName().getString(),
               active.getAmplifier() + 1,
               amplifier + 1,
               cap + 1
            );
         }

         cir.setReturnValue(new MobEffectInstance(effect, this.effectInstance.getDuration(), amplifier));
      }
   }
}
