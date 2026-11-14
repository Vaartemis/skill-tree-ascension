package dev.skillascension.mixin;

import dev.skillascension.pst.EnchantingBonuses;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Enchanting Discount on the vanilla table: fewer levels charged. (SRG: clickMenuButton lambda, Player.onEnchantmentPerformed.) */
@Mixin(value = EnchantmentMenu.class, remap = false)
public abstract class EnchantTableDiscountMixin {
    @Redirect(method = "m_39475_", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/entity/player/Player;m_7408_(Lnet/minecraft/world/item/ItemStack;I)V"))
    private void skillascension$discount(Player player, ItemStack stack, int levels) {
        player.onEnchantmentPerformed(stack, EnchantingBonuses.discountedCost(player, levels));
    }
}
