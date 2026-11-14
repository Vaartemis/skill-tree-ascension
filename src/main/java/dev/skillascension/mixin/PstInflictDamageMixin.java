package dev.skillascension.mixin;

import daripher.skilltree.skill.bonus.predicate.damage.DamageCondition;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.skill.bonus.player.InflictDamageBonus"},
   remap = false
)
public abstract class PstInflictDamageMixin {
   @Unique
   private static final ThreadLocal<int[]> SKILLASCENSION$DEPTH = ThreadLocal.withInitial(() -> new int[1]);
   @Shadow
   private float damage;
   @Shadow
   private DamageCondition damageType;

   @Inject(
      method = {"applyEffect"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private void skillascension$typedDamage(LivingEntity var1, LivingEntity var2, CallbackInfo var3) {
      int[] var4 = SKILLASCENSION$DEPTH.get();
      if (var4[0] > 0) {
         var3.cancel();
      } else {
         var3.cancel();
         var4[0]++;

         try {
            DamageSource var5 = null;
            if (var2 instanceof Player var6 && this.damageType != null && this.damageType.canCreateDamageSource()) {
               try {
                  var5 = this.damageType.createDamageSource(var6);
               } catch (RuntimeException var11) {
               }
            }

            if (var5 == null) {
               var5 = var1.level().damageSources().magic();
            }

            var1.hurt(var5, this.damage);
         } finally {
            var4[0]--;
         }
      }
   }
}
