package dev.skillascension.mixin;

import dev.skillascension.pst.EnchantingBonuses;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Enchanting Discount on Apotheosis' enchanting table, which charges experience points (optional). */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu", remap = false)
public abstract class ApothEnchantDiscountMixin {
    @ModifyArg(method = "lambda$clickMenuButton$0", remap = false, require = 0, index = 1,
            at = @At(value = "INVOKE", remap = false,
                    target = "Ldev/shadowsoffire/placebo/util/EnchantmentUtils;chargeExperience(Lnet/minecraft/world/entity/player/Player;I)Z"))
    private int skillascension$discount(Player player, int cost) {
        return EnchantingBonuses.discountedCost(player, cost);
    }
}
