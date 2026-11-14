package dev.skillascension.pst.function;

import com.google.gson.JsonObject;
import daripher.skilltree.client.widget.editor.SkillTreeEditor;
import daripher.skilltree.skill.bonus.SkillBonus.Target;
import daripher.skilltree.skill.bonus.function.FloatFunction;
import daripher.skilltree.skill.bonus.predicate.living.FloatFunctionEntityPredicate.Logic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

public final class SpellPowerSumFunction implements FloatFunction<SpellPowerSumFunction> {
   private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer;
   private final float min;
   private static List<Attribute> schools;

   public SpellPowerSumFunction(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer, float min) {
      this.serializer = serializer;
      this.min = min;
   }

   public float apply(LivingEntity entity) {
      if (!(entity instanceof Player player)) {
         return 0.0F;
      } else {
         if (schools == null) {
            List<Attribute> list = new ArrayList<>();

            for (Entry<ResourceKey<Attribute>, Attribute> e : ForgeRegistries.ATTRIBUTES.getEntries()) {
               if (e.getKey().location().getPath().endsWith("_spell_power")) {
                  list.add(e.getValue());
               }
            }

            schools = list;
         }

         double sum = 0.0;
         double generic = value(player, (Attribute)ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks", "spell_power")), 0.0);
         if (generic >= (double)this.min) {
            sum += generic;
         }

         for (Attribute a : schools) {
            double v = value(player, a, 0.0);
            if (v >= (double)this.min) {
               sum += v;
            }
         }

         return (float)sum;
      }
   }

   private static double value(Player player, Attribute attribute, double fallback) {
      if (attribute == null) {
         return fallback;
      } else {
         AttributeInstance inst = player.getAttribute(attribute);
         return inst == null ? fallback : inst.getValue();
      }
   }

   public MutableComponent getMultiplierTooltip(Target target, float divisor, Component bonus) {
      String key = "skillascension.value.spell_power_sum.multiplier";
      return Component.translatable(key, new Object[]{bonus, this.formatNumber(divisor * 100.0F), this.formatNumber(this.min * 100.0F)});
   }

   public MutableComponent getPredicateTooltip(Target target, Logic logic, Component bonus, float value) {
      return Component.translatable("skillascension.value.spell_power_sum.condition", new Object[]{bonus, this.formatNumber(value * 100.0F)});
   }

   public MutableComponent getRequirementTooltip(Logic logic, float value) {
      return Component.translatable("skillascension.value.spell_power_sum.requirement", new Object[]{this.formatNumber(value * 100.0F)});
   }

   public daripher.skilltree.skill.bonus.function.FloatFunction.Serializer getSerializer() {
      return this.serializer.get();
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<FloatFunction<?>> consumer) {
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof SpellPowerSumFunction f && f.min == this.min) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return 31 + Float.hashCode(this.min);
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.function.FloatFunction.Serializer {
      private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self;

      public Serializer(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self) {
         this.self = self;
      }

      public FloatFunction<?> deserialize(JsonObject json) {
         return new SpellPowerSumFunction(this.self, json.has("min") ? json.get("min").getAsFloat() : 1.25F);
      }

      public void serialize(JsonObject json, FloatFunction<?> f) {
         json.addProperty("min", ((SpellPowerSumFunction)f).min);
      }

      public FloatFunction<?> deserialize(CompoundTag tag) {
         return new SpellPowerSumFunction(this.self, tag.getFloat("min"));
      }

      public CompoundTag serialize(FloatFunction<?> f) {
         CompoundTag tag = new CompoundTag();
         tag.putFloat("min", ((SpellPowerSumFunction)f).min);
         return tag;
      }

      public FloatFunction<?> deserialize(FriendlyByteBuf buf) {
         return new SpellPowerSumFunction(this.self, buf.readFloat());
      }

      public void serialize(FriendlyByteBuf buf, FloatFunction<?> f) {
         buf.writeFloat(((SpellPowerSumFunction)f).min);
      }

      public FloatFunction<?> createDefaultInstance() {
         return new SpellPowerSumFunction(this.self, 1.25F);
      }
   }
}
