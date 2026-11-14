package dev.skillascension.mixin;

import dev.skillascension.pst.EnchantingBonuses;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Same as the vanilla one, for Apotheosis' enchanting table (optional). */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.advancements.EnchantedTrigger", remap = false)
public abstract class ApothEnchantAmplifyMixin {
    @Inject(method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;IFFFF)V",
            at = @At("HEAD"), remap = false, require = 0)
    private void skillascension$amplify(ServerPlayer player, ItemStack stack, int level, float eterna, float quanta, float arcana, float rectification, CallbackInfo ci) {
        EnchantingBonuses.amplify(player, stack);
    }
}
