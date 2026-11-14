package dev.skillascension.compat;

import dev.skillascension.compat.irons.IronsCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.fml.ModList;

/** Optional mod support. Classes of other mods are only touched when the mod is loaded. */
public final class Compat {
    public static final boolean IRONS = ModList.get().isLoaded("irons_spellbooks");
    public static final boolean APOTHEOSIS = ModList.get().isLoaded("apotheosis");

    private Compat() {
    }

    public static boolean isSpellDamage(DamageSource source) {
        return IRONS && IronsCompat.isSpellDamage(source);
    }

    public static int gemCount(net.minecraft.world.item.ItemStack stack) {
        if (!APOTHEOSIS) return 0;
        try {
            return dev.skillascension.compat.apotheosis.ApotheosisCompat.gemCount(stack);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void ironsTick(ServerPlayer player) {
        if (IRONS) IronsCompat.tick(player);
    }
}
