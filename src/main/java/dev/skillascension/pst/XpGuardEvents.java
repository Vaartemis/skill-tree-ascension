package dev.skillascension.pst;

import daripher.skilltree.skill.SkillBonusProvider;
import dev.skillascension.pst.bonus.GemPowerBonus;
import dev.skillascension.pst.bonus.XpAegisBonus;
import dev.skillascension.pst.bonus.XpSiphonBonus;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** Experience Aegis and Experience Siphon bonuses. */
public final class XpGuardEvents {
   private XpGuardEvents() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player && !(event.getAmount() <= 0.0F)) {
         float share = 0.0F;

         for (XpAegisBonus bonus : SkillBonusProvider.getSkillBonuses(player, XpAegisBonus.class)) {
            share += bonus.valueFor(player, null);
         }

         if (share <= 0.0F) {
            return;
         }

         float absorbed = event.getAmount() * Math.min(0.6F, share);
         int cost = (int)Math.ceil((double)(absorbed * 2.0F));
         if (cost > 0 && player.totalExperience >= cost) {
            player.giveExperiencePoints(-cost);
            event.setAmount(event.getAmount() - absorbed);
            return;
         }

         return;
      }
   }

   @SubscribeEvent
   public static void onPickupXp(PickupXp event) {
      Player player = event.getEntity();
      if (!player.level().isClientSide) {
         float heal = 0.0F;

         for (XpSiphonBonus bonus : SkillBonusProvider.getSkillBonuses(player, XpSiphonBonus.class)) {
            heal += bonus.valueFor(player, null);
         }

         if (heal > 0.0F) {
            player.heal(heal * (float)event.getOrb().getValue());
         }
      }
   }
}
