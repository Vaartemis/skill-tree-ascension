package dev.skillascension.compat.apotheosis;

import dev.skillascension.pst.bonus.GemPowerBonus;
import dev.skillascension.pst.bonus.XpAegisBonus;
import dev.skillascension.pst.bonus.XpSiphonBonus;
import daripher.skilltree.skill.SkillBonusProvider;
import dev.shadowsoffire.apotheosis.adventure.loot.LootCategory;
import dev.shadowsoffire.apotheosis.adventure.socket.SocketHelper;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
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

public final class GemEvents {
   private static final boolean APOTHEOSIS = true; // only registered when Apotheosis is loaded
   private static final Map<UUID, Set<UUID>> APPLIED = new HashMap<>();
   private static final Map<UUID, Attribute> MODIFIER_ATTRIBUTE = new HashMap<>();
   private static final Map<UUID, Operation> OPS = new HashMap<>();

   private GemEvents() {
   }

   @SubscribeEvent
   public static void onTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && event.player instanceof ServerPlayer player && player.tickCount % 20 == 0) {
         try {
            updateGemPower(player);
         } catch (Throwable var3) {
         }
      }
   }

   @SubscribeEvent
   public static void onLogout(PlayerLoggedOutEvent event) {
      APPLIED.remove(event.getEntity().getUUID());
   }

   private static void updateGemPower(ServerPlayer player) {
      List<GemPowerBonus> bonuses = SkillBonusProvider.getSkillBonuses(player, GemPowerBonus.class);
      Map<UUID, double[]> sums = new HashMap<>();
      if (APOTHEOSIS && !bonuses.isEmpty()) {
         for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
               float power = 0.0F;

               for (GemPowerBonus bonus : bonuses) {
                  power += bonus.valueFor(player, stack);
               }

               if (!(power <= 0.0F)) {
                  LootCategory category = LootCategory.forItem(stack);
                  if (category != null && !category.isNone()) {
                     float p = power;
                     SocketHelper.getGems(stack).addModifiers(category, slot, (attribute, modifier) -> {
                        UUID id = modifierId(attribute, modifier.getOperation());
                        MODIFIER_ATTRIBUTE.put(id, attribute);
                        sums.computeIfAbsent(id, k -> new double[1])[0] += modifier.getAmount() * (double)p;
                     });
                  }
               }
            }
         }
      }

      Set<UUID> before = APPLIED.getOrDefault(player.getUUID(), Set.of());
      Set<UUID> now = new HashSet<>();

      for (UUID id : before) {
         if (!sums.containsKey(id)) {
            AttributeInstance instance = instance(player, id);
            if (instance != null) {
               instance.removeModifier(id);
            }
         }
      }

      for (Entry<UUID, double[]> e : sums.entrySet()) {
         AttributeInstance instance = instance(player, e.getKey());
         if (instance != null) {
            Operation op = operation(e.getKey());
            AttributeModifier old = instance.getModifier(e.getKey());
            if (old != null && Math.abs(old.getAmount() - e.getValue()[0]) < 1.0E-6) {
               now.add(e.getKey());
            } else {
               instance.removeModifier(e.getKey());
               instance.addTransientModifier(new AttributeModifier(e.getKey(), "Skill gem power", e.getValue()[0], op));
               now.add(e.getKey());
            }
         }
      }

      if (now.isEmpty()) {
         APPLIED.remove(player.getUUID());
      } else {
         APPLIED.put(player.getUUID(), now);
      }
   }

   private static UUID modifierId(Attribute attribute, Operation op) {
      UUID id = UUID.nameUUIDFromBytes(
         ("skillascension:gem_power/" + ForgeRegistries.ATTRIBUTES.getKey(attribute) + "/" + op.name()).getBytes(StandardCharsets.UTF_8)
      );
      OPS.put(id, op);
      return id;
   }

   private static Operation operation(UUID id) {
      return OPS.getOrDefault(id, Operation.ADDITION);
   }

   private static AttributeInstance instance(Player player, UUID id) {
      Attribute attribute = MODIFIER_ATTRIBUTE.get(id);
      return attribute == null ? null : player.getAttribute(attribute);
   }
}
