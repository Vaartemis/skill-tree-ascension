package dev.skillascension.mixin;

import dev.skillascension.pst.EnchantingBonuses;
import net.minecraft.advancements.critereon.EnchantedItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bonus enchantments and enchantment amplification after enchanting on the vanilla table. (SRG: EnchantedItemTrigger.trigger.) */
@Mixin(value = EnchantedItemTrigger.class, remap = false)
public abstract class EnchantTriggerAmplifyMixin {
    @Inject(method = "m_27668_", at = @At("HEAD"), remap = false, require = 0)
    private void skillascension$amplify(ServerPlayer player, ItemStack stack, int levels, CallbackInfo ci) {
        EnchantingBonuses.amplify(player, stack);
    }
}
