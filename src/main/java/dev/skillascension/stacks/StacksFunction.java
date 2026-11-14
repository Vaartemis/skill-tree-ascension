package dev.skillascension.stacks;

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

public final class StacksFunction implements FloatFunction<StacksFunction> {
   private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer;
   private final String stack;

   public StacksFunction(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> var1, String var2) {
      this.serializer = var1;
      this.stack = var2;
   }

   public float apply(LivingEntity var1) {
      if (var1 instanceof Player var2) {
         if (!StacksLog.known(this.stack) && !this.stack.endsWith("*") && Counters.formula(var2, this.stack) == null) {
            StacksLog.warnOnce(
               "node:" + this.stack, "A skill node reads stack '{}', which is not a counter, raw stack or stacks.json entry (always 0)", this.stack
            );
         }

         return (float)EternalStacks.get(var2, this.stack);
      } else {
         return 0.0F;
      }
   }

   private Component name() {
      return Component.translatable("skillascension.stacks." + this.stack.replace(':', '.').replace(".*", ".all"));
   }

   public MutableComponent getMultiplierTooltip(Target var1, float var2, Component var3) {
      return Component.translatable("skillascension.value.stacks.multiplier", new Object[]{var3, this.formatNumber(var2), this.name()});
   }

   public MutableComponent getPredicateTooltip(Target var1, Logic var2, Component var3, float var4) {
      return Component.translatable("skillascension.value.stacks.condition", new Object[]{var3, this.formatNumber(var4), this.name()});
   }

   public MutableComponent getRequirementTooltip(Logic var1, float var2) {
      return Component.translatable("skillascension.value.stacks.requirement", new Object[]{this.formatNumber(var2), this.name()});
   }

   public daripher.skilltree.skill.bonus.function.FloatFunction.Serializer getSerializer() {
      return this.serializer.get();
   }

   public void addEditorWidgets(SkillTreeEditor var1, Consumer<FloatFunction<?>> var2) {
   }

   @Override
   public boolean equals(Object var1) {
      if (var1 instanceof StacksFunction var2 && var2.stack.equals(this.stack)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.stack.hashCode();
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.function.FloatFunction.Serializer {
      private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self;

      public Serializer(Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> var1) {
         this.self = var1;
      }

      public FloatFunction<?> deserialize(JsonObject var1) {
         return new StacksFunction(this.self, var1.has("stack") ? var1.get("stack").getAsString() : "trophies");
      }

      public void serialize(JsonObject var1, FloatFunction<?> var2) {
         var1.addProperty("stack", ((StacksFunction)var2).stack);
      }

      public FloatFunction<?> deserialize(CompoundTag var1) {
         return new StacksFunction(this.self, var1.getString("stack"));
      }

      public CompoundTag serialize(FloatFunction<?> var1) {
         CompoundTag var2 = new CompoundTag();
         var2.putString("stack", ((StacksFunction)var1).stack);
         return var2;
      }

      public FloatFunction<?> deserialize(FriendlyByteBuf var1) {
         return new StacksFunction(this.self, var1.readUtf(128));
      }

      public void serialize(FriendlyByteBuf var1, FloatFunction<?> var2) {
         var1.writeUtf(((StacksFunction)var2).stack, 128);
      }

      public FloatFunction<?> createDefaultInstance() {
         return new StacksFunction(this.self, "trophies");
      }
   }
}
