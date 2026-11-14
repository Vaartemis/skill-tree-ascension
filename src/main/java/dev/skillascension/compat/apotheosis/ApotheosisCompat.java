package dev.skillascension.compat.apotheosis;

import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import net.minecraft.world.item.ItemStack;

/** Apotheosis parts. Only loaded when Apotheosis is installed. */
public final class ApotheosisCompat {
    private ApotheosisCompat() {
    }

    public static int gemCount(ItemStack stack) {
        return (int) SocketHelper.getGems(stack).streamValidGems().count();
    }
}
