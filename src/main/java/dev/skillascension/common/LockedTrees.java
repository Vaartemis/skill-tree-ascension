package dev.skillascension.common;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** Client copy of the trees the server keeps from this player (see TreeAccess). */
public final class LockedTrees {
    private static volatile Set<ResourceLocation> locked = Set.of();

    private LockedTrees() {
    }

    public static void set(Set<ResourceLocation> trees) {
        locked = Set.copyOf(trees);
    }

    public static boolean isLocked(ResourceLocation tree) {
        return tree != null && locked.contains(tree);
    }
}
