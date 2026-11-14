package dev.skillascension.pst.function;

import com.google.gson.JsonObject;
import daripher.skilltree.client.widget.editor.SkillTreeEditor;
import daripher.skilltree.skill.bonus.SkillBonus.Target;
import daripher.skilltree.skill.bonus.function.FloatFunction;
import daripher.skilltree.skill.bonus.predicate.living.FloatFunctionEntityPredicate.Logic;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ExperienceLevelFunction implements FloatFunction<ExperienceLevelFunction> {
   private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer;
   private final int max;

   public ExperienceLevelFunction(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer, int max) {
      this.serializer = serializer;
      this.max = max;
   }

   public float apply(LivingEntity entity) {
      if (entity instanceof Player player) {
         int level = player.experienceLevel;
         return this.max > 0 ? (float)Math.min(level, this.max) : (float)level;
      } else {
         return 0.0F;
      }
   }

   public MutableComponent getMultiplierTooltip(Target target, float divisor, Component bonus) {
      String key = this.max > 0 ? "skillascension.value.experience_level.multiplier_max" : "skillascension.value.experience_level.multiplier";
      return Component.translatable(key, new Object[]{bonus, this.formatNumber(divisor), this.max});
   }

   public MutableComponent getPredicateTooltip(Target target, Logic logic, Component bonus, float value) {
      return Component.translatable("skillascension.value.experience_level.condition", new Object[]{bonus, this.formatNumber(value)});
   }

   public MutableComponent getRequirementTooltip(Logic logic, float value) {
      return Component.translatable("skillascension.value.experience_level.requirement", new Object[]{this.formatNumber(value)});
   }

   public daripher.skilltree.skill.bonus.function.FloatFunction.Serializer getSerializer() {
      return this.serializer.get();
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<FloatFunction<?>> consumer) {
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof ExperienceLevelFunction f && f.max == this.max) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return 31 + this.max;
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.function.FloatFunction.Serializer {
      private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self;

      public Serializer(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self) {
         this.self = self;
      }

      public FloatFunction<?> deserialize(JsonObject json) {
         return new ExperienceLevelFunction(this.self, json.has("max") ? json.get("max").getAsInt() : 0);
      }

      public void serialize(JsonObject json, FloatFunction<?> f) {
         json.addProperty("max", ((ExperienceLevelFunction)f).max);
      }

      public FloatFunction<?> deserialize(CompoundTag tag) {
         return new ExperienceLevelFunction(this.self, tag.getInt("max"));
      }

      public CompoundTag serialize(FloatFunction<?> f) {
         CompoundTag tag = new CompoundTag();
         tag.putInt("max", ((ExperienceLevelFunction)f).max);
         return tag;
      }

      public FloatFunction<?> deserialize(FriendlyByteBuf buf) {
         return new ExperienceLevelFunction(this.self, buf.readInt());
      }

      public void serialize(FriendlyByteBuf buf, FloatFunction<?> f) {
         buf.writeInt(((ExperienceLevelFunction)f).max);
      }

      public FloatFunction<?> createDefaultInstance() {
         return new ExperienceLevelFunction(this.self, 0);
      }
   }
}
