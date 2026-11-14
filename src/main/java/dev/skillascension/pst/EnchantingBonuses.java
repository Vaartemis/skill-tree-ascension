package dev.skillascension.pst;

import dev.skillascension.pst.bonus.BonusEnchantmentBonus;
import dev.skillascension.pst.bonus.EnchantingDiscountBonus;
import dev.skillascension.pst.bonus.EnchantmentAmplificationBonus;
import daripher.skilltree.skill.SkillBonusProvider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

public final class EnchantingBonuses {
   private EnchantingBonuses() {
   }

   public static int discountedCost(Player player, int cost) {
      if (player != null && cost > 0) {
         float discount = 0.0F;

         for (EnchantingDiscountBonus bonus : SkillBonusProvider.getSkillBonuses(player, EnchantingDiscountBonus.class)) {
            discount += bonus.valueFor(player, null);
         }

         return discount <= 0.0F ? cost : Math.max(1, Math.round((float)cost * (1.0F - Math.min(0.9F, discount))));
      } else {
         return cost;
      }
   }

   public static float amplificationChance(Player player, ItemStack stack) {
      float chance = 0.0F;

      for (EnchantmentAmplificationBonus bonus : SkillBonusProvider.getSkillBonuses(player, EnchantmentAmplificationBonus.class)) {
         chance += bonus.valueFor(player, stack);
      }

      return chance;
   }

   public static float bonusEnchantmentChance(Player player, ItemStack stack) {
      float chance = 0.0F;

      for (BonusEnchantmentBonus bonus : SkillBonusProvider.getSkillBonuses(player, BonusEnchantmentBonus.class)) {
         chance += bonus.valueFor(player, stack);
      }

      return chance;
   }

   private static int roll(RandomSource random, float chance) {
      int n = (int)chance;
      if (random.nextFloat() < chance - (float)n) {
         n++;
      }

      return n;
   }

   public static void amplify(ServerPlayer player, ItemStack fallback) {
      ItemStack stack = player.containerMenu instanceof EnchantmentMenu menu ? menu.getSlot(0).getItem() : fallback;
      if (stack != null && !stack.isEmpty()) {
         Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
         if (!enchantments.isEmpty()) {
            RandomSource random = player.getRandom();
            boolean changed = false;
            int extra = roll(random, bonusEnchantmentChance(player, stack));

            for (int i = 0; i < extra; i++) {
               Enchantment pick = pickExtra(stack, enchantments, random);
               if (pick == null) {
                  break;
               }

               enchantments.put(pick, 1 + random.nextInt(Math.max(1, pick.getMaxLevel())));
               changed = true;
            }

            float chance = amplificationChance(player, stack);
            if (chance > 0.0F) {
               for (Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                  int up = roll(random, chance);
                  if (up > 0) {
                     entry.setValue(entry.getValue() + up);
                     changed = true;
                  }
               }
            }

            if (changed) {
               EnchantmentHelper.setEnchantments(enchantments, stack);
            }
         }
      }
   }

   private static Enchantment pickExtra(ItemStack stack, Map<Enchantment, Integer> current, RandomSource random) {
      boolean book = stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.BOOK);
      List<Enchantment> options = new ArrayList<>();

      for (Enchantment e : ForgeRegistries.ENCHANTMENTS.getValues()) {
         if (!current.containsKey(e)
            && !e.isCurse()
            && !e.isTreasureOnly()
            && e.isDiscoverable()
            && (book || e.canEnchant(stack))
            && EnchantmentHelper.isEnchantmentCompatible(current.keySet(), e)) {
            options.add(e);
         }
      }

      return options.isEmpty() ? null : options.get(random.nextInt(options.size()));
   }
}
