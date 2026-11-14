package dev.skillascension.mixin;

import dev.skillascension.pst.DeathKnowledgeEvents;
import dev.skillascension.pst.bonus.EventBonus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/** Night Vigil: players with the bonus can sleep with monsters nearby. (SRG names: ServerPlayer.startSleepInBed, Level.getEntitiesOfClass.) */
@Mixin(ServerPlayer.class)
public abstract class NightVigilSleepMixin {
    @Redirect(method = "m_7720_", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lnet/minecraft/world/level/Level;m_6443_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private <T extends Entity> List<T> skillascension$nightVigil(Level level, Class<T> type, AABB box, Predicate<? super T> filter) {
        return DeathKnowledgeEvents.amount((ServerPlayer) (Object) this, EventBonus.Kind.NIGHT_VIGIL) > 0.0F
                ? Collections.emptyList() : level.getEntitiesOfClass(type, box, filter);
    }
}
