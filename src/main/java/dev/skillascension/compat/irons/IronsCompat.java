package dev.skillascension.compat.irons;

import dev.skillascension.pst.ClassEvents;
import dev.skillascension.pst.CombatEvents;
import dev.skillascension.pst.DeathKnowledgeEvents;
import dev.skillascension.pst.bonus.EventBonus;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Iron's Spells 'n Spellbooks parts of the bonuses. Only registered when that mod is installed. */
public final class IronsCompat {
   private static final Map<UUID, Object[]> ECHO = new HashMap<>();

   private IronsCompat() {
   }

   public static boolean isSpellDamage(DamageSource source) {
      return source instanceof SpellDamageSource;
   }

   @SubscribeEvent
   public static void onCastEcho(SpellOnCastEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         try {
            float[] echo = ClassEvents.get(player, EventBonus.Kind.ARCANE_ECHO);
            if (echo == null || player.getRandom().nextFloat() >= Math.min(0.35F, echo[0])) {
               return;
            }

            event.setManaCost(0);
            ECHO.put(player.getUUID(), new Object[]{event.getSpellId(), 200});
         } catch (Throwable var3) {
         }
      }
   }


   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public static void onCastCombat(SpellOnCastEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         try {
            long now = dev.skillascension.common.Clock.now();
            UUID id = player.getUUID();
            float[] flow = CombatEvents.get(player, EventBonus.Kind.MANA_FLOW);
            if (flow != null && event.getManaCost() > 0) {
               double[] st = CombatEvents.FLOW.computeIfAbsent(id, k -> new double[3]);
               if ((double)now > st[2]) {
                  st[1] = 0.0;
               }

               st[0] += (double)event.getManaCost();

               for (float step = Math.max(10.0F, flow[1] > 0.0F ? flow[1] : 100.0F);
                  st[0] >= (double)step;
                  st[2] = (double)(now + (long)((flow[2] > 0.0F ? flow[2] : 6.0F) * 1000.0F))
               ) {
                  st[0] -= (double)step;
                  st[1] = Math.min(10.0, st[1] + 1.0);
               }
            }

            float[] oc = CombatEvents.get(player, EventBonus.Kind.OVERCHANNEL);
            if (oc != null) {
               float max = (float)player.getAttributeValue((Attribute)AttributeRegistry.MAX_MANA.get());
               float mana = MagicData.getPlayerMagicData(player).getMana();
               if (max > 0.0F && mana < max * (oc[1] > 0.0F ? oc[1] : 25.0F) / 100.0F) {
                  CombatEvents.CHANNEL.put(id, now + 3000L);
               }
            }

            float[] ch = CombatEvents.get(player, EventBonus.Kind.SCHOOL_CHAIN);
            if (ch != null && event.getSchoolType() != null) {
               String school = event.getSchoolType().getId().toString();
               double[] st = CombatEvents.CHAIN.computeIfAbsent(id, k -> new double[2]);
               String last = CombatEvents.LAST_SCHOOL.put(id, school);
               if ((double)now > st[1]) {
                  st[0] = 0.0;
               }

               if (last != null && !last.equals(school)) {
                  st[0] = Math.min(ch[1] > 0.0F ? (double)ch[1] : 5.0, st[0] + 1.0);
               }

               st[1] = (double)(now + (long)((ch[2] > 0.0F ? ch[2] : 6.0F) * 1000.0F));
            }
         } catch (Throwable var11) {
         }
      }
   }


   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onCastScroll(SpellOnCastEvent var0) {
      if (!var0.isCanceled() && var0.getCastSource() == CastSource.SCROLL) {
         if (!(var0.getEntity() instanceof ServerPlayer var1) || var1.getAbilities().instabuild) {
            return;
         }

         if (DeathKnowledgeEvents.roll(var1, DeathKnowledgeEvents.amount(var1, EventBonus.Kind.RUNE_SCRIBE))) {
            for (InteractionHand var5 : InteractionHand.values()) {
               ItemStack var6 = var1.getItemInHand(var5);
               if (var6.getItem() instanceof Scroll) {
                  ItemStack var7 = var6.copy();
                  var7.setCount(1);
                  DeathKnowledgeEvents.giveBack(var1, var7, var5);
                  return;
               }
            }
         }
      }
   }


   /** Arcane Echo: refunds the cooldown of the spell that was cast for free. */
   public static void tick(ServerPlayer player) {
      Object[] echo = ECHO.get(player.getUUID());
         if (echo != null) {
            try {
               MagicData data = MagicData.getPlayerMagicData(player);
               String id = (String)echo[0];
               if (data.getPlayerCooldowns().getSpellCooldowns().containsKey(id)) {
                  data.getPlayerCooldowns().removeCooldown(id);
                  data.getPlayerCooldowns().syncToPlayer(player);
                  ECHO.remove(player.getUUID());
                  player.displayClientMessage(Component.translatable("skillascension.arcane_echo.proc").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                  player.level()
                     .playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
                  ((ServerLevel)player.level())
                     .sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getEyeY(), player.getZ(), 20, 0.5, 0.5, 0.5, 0.5);
               } else {
                  int left = (Integer)echo[1] - 1;
                  if (left <= 0) {
                     ECHO.remove(player.getUUID());
                  } else {
                     echo[1] = left;
                  }
               }
            } catch (Throwable var8) {
               ECHO.remove(player.getUUID());
            }
         }

   }
}
