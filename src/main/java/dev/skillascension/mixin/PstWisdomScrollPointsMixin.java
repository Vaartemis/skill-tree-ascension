package dev.skillascension.mixin;

import dev.skillascension.points.PointSources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A Wisdom Scroll gives skill points, which can unlock "skill_points_earned" point sources. */
@Pseudo
@Mixin(targets = "daripher.skilltree.item.WisdomScrollItem", remap = false)
public abstract class PstWisdomScrollPointsMixin {
    @Inject(method = "m_7203_", at = @At("RETURN"), remap = false, require = 0)
    private void skillascension$updatePoints(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (player instanceof ServerPlayer serverPlayer) PointSources.update(serverPlayer);
    }
}
