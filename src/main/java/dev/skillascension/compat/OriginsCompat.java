package dev.skillascension.compat;

import dev.skillascension.SkillAscension;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Origins (Forge) and Apoli, read by reflection so neither is needed to run.
 * Origins: IOriginContainer.get(entity).getOrigins(); Apoli: IPowerContainer.get(entity).hasPower(id).
 */
public final class OriginsCompat {
    private static Method originGet;
    private static Method originsMap;
    private static Method powerGet;
    private static Method hasPower;
    private static boolean triedOrigins;
    private static boolean triedPowers;

    private OriginsCompat() {
    }

    private static synchronized boolean originsReady() {
        if (!triedOrigins) {
            triedOrigins = true;
            if (ModList.get().isLoaded("origins")) {
                try {
                    Class<?> c = Class.forName("io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer");
                    originGet = c.getMethod("get", Entity.class);
                    originsMap = c.getMethod("getOrigins");
                } catch (Throwable t) {
                    SkillAscension.LOG.warn("Origins is installed but its API was not found, origin conditions will be false: {}", t.toString());
                }
            }
        }
        return originGet != null;
    }

    private static synchronized boolean powersReady() {
        if (!triedPowers) {
            triedPowers = true;
            if (ModList.get().isLoaded("apoli")) {
                try {
                    Class<?> c = Class.forName("io.github.edwinmindcraft.apoli.api.component.IPowerContainer");
                    powerGet = c.getMethod("get", Entity.class);
                    hasPower = c.getMethod("hasPower", ResourceLocation.class);
                } catch (Throwable t) {
                    SkillAscension.LOG.warn("Apoli is installed but its API was not found, power conditions will be false: {}", t.toString());
                }
            }
        }
        return powerGet != null;
    }

    private static Object resolve(Method getter, Entity entity) throws Exception {
        Object lazy = getter.invoke(null, entity);
        if (lazy instanceof LazyOptional<?> opt) return opt.resolve().orElse(null);
        return null;
    }

    /** True when the entity has this origin (in this layer, or in any layer when layer is null). */
    public static boolean hasOrigin(Entity entity, ResourceLocation origin, ResourceLocation layer) {
        if (origin == null || !originsReady()) return false;
        try {
            Object container = resolve(originGet, entity);
            if (container == null) return false;
            Map<?, ?> map = (Map<?, ?>) originsMap.invoke(container);
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!(e.getValue() instanceof ResourceKey<?> value) || !origin.equals(value.location())) continue;
                if (layer == null || (e.getKey() instanceof ResourceKey<?> key && layer.equals(key.location()))) return true;
            }
        } catch (Throwable t) {
            SkillAscension.LOG.debug("Origin check failed: {}", t.toString());
        }
        return false;
    }

    /** True when the entity has this Apoli power (from an origin or any other source). */
    public static boolean hasPower(Entity entity, ResourceLocation power) {
        if (power == null || !powersReady()) return false;
        try {
            Object container = resolve(powerGet, entity);
            return container != null && Boolean.TRUE.equals(hasPower.invoke(container, power));
        } catch (Throwable t) {
            SkillAscension.LOG.debug("Power check failed: {}", t.toString());
            return false;
        }
    }
}
