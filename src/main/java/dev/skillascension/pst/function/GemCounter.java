package dev.skillascension.pst.function;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.ModList;

public final class GemCounter {

   private GemCounter() {
   }

   public static int gems(ItemStack stack) {
      return dev.skillascension.compat.Compat.gemCount(stack);
   }

   public static int curses(ItemStack stack) {
      int n = 0;

      for (Enchantment e : EnchantmentHelper.getEnchantments(stack).keySet()) {
         if (e.isCurse()) {
            n++;
         }
      }

      return n;
   }
}
