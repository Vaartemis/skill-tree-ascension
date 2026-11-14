package dev.skillascension.pst.function;

import com.google.gson.JsonObject;
import daripher.skilltree.client.widget.editor.SkillTreeEditor;
import daripher.skilltree.data.serializers.SerializationHelper;
import daripher.skilltree.network.NetworkHelper;
import daripher.skilltree.skill.bonus.SkillBonus.Target;
import daripher.skilltree.skill.bonus.function.FloatFunction;
import daripher.skilltree.skill.bonus.predicate.item.ItemStackPredicate;
import daripher.skilltree.skill.bonus.predicate.item.NoneItemStackPredicate;
import daripher.skilltree.skill.bonus.predicate.living.FloatFunctionEntityPredicate.Logic;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class EquipmentCountFunction implements FloatFunction<EquipmentCountFunction> {
   private final EquipmentCountFunction.Kind kind;
   private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer;
   private ItemStackPredicate item;

   public EquipmentCountFunction(
      EquipmentCountFunction.Kind kind, Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> serializer, ItemStackPredicate item
   ) {
      this.kind = kind;
      this.serializer = serializer;
      this.item = item;
   }

   public float apply(LivingEntity entity) {
      int total = 0;

      for (EquipmentSlot slot : EquipmentSlot.values()) {
         ItemStack stack = entity.getItemBySlot(slot);
         if (!stack.isEmpty() && this.item.test(stack)) {
            total += this.kind == EquipmentCountFunction.Kind.GEMS ? GemCounter.gems(stack) : GemCounter.curses(stack);
         }
      }

      return (float)total;
   }

   private String key() {
      return this.kind == EquipmentCountFunction.Kind.GEMS ? "skillascension.value.gems_amount" : "skillascension.value.curse_amount";
   }

   public MutableComponent getMultiplierTooltip(Target target, float divisor, Component bonus) {
      return Component.translatable(this.key() + ".multiplier", new Object[]{bonus, this.item.getTooltip("plural")});
   }

   public MutableComponent getPredicateTooltip(Target target, Logic logic, Component bonus, float value) {
      return Component.translatable(this.key() + ".condition", new Object[]{bonus, this.formatNumber(value), this.item.getTooltip("plural")});
   }

   public MutableComponent getRequirementTooltip(Logic logic, float value) {
      return Component.translatable(this.key() + ".requirement", new Object[]{this.formatNumber(value), this.item.getTooltip("plural")});
   }

   public daripher.skilltree.skill.bonus.function.FloatFunction.Serializer getSerializer() {
      return this.serializer.get();
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<FloatFunction<?>> consumer) {
   }

   @Override
   public boolean equals(Object o) {
      if (o instanceof EquipmentCountFunction f && f.kind == this.kind && f.item.equals(this.item)) {
         return true;
      }

      return false;
   }

   @Override
   public int hashCode() {
      return this.kind.hashCode() * 31 + this.item.hashCode();
   }

   public static enum Kind {
      GEMS,
      CURSES;
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.function.FloatFunction.Serializer {
      private final EquipmentCountFunction.Kind kind;
      private final Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self;

      public Serializer(EquipmentCountFunction.Kind kind, Supplier<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> self) {
         this.kind = kind;
         this.self = self;
      }

      public FloatFunction<?> deserialize(JsonObject json) {
         ItemStackPredicate item = (ItemStackPredicate)(json.has("item_condition")
            ? SerializationHelper.deserializeItemPredicate(json, "item_condition")
            : NoneItemStackPredicate.INSTANCE);
         return new EquipmentCountFunction(this.kind, this.self, item);
      }

      public void serialize(JsonObject json, FloatFunction<?> f) {
         SerializationHelper.serializeItemPredicate(json, ((EquipmentCountFunction)f).item, "item_condition");
      }

      public FloatFunction<?> deserialize(CompoundTag tag) {
         ItemStackPredicate item = (ItemStackPredicate)(tag.contains("item_condition")
            ? SerializationHelper.deserializeItemPredicate(tag)
            : NoneItemStackPredicate.INSTANCE);
         return new EquipmentCountFunction(this.kind, this.self, item);
      }

      public CompoundTag serialize(FloatFunction<?> f) {
         CompoundTag tag = new CompoundTag();
         SerializationHelper.serializeItemPredicate(tag, ((EquipmentCountFunction)f).item);
         return tag;
      }

      public FloatFunction<?> deserialize(FriendlyByteBuf buf) {
         return new EquipmentCountFunction(this.kind, this.self, NetworkHelper.readItemPredicate(buf));
      }

      public void serialize(FriendlyByteBuf buf, FloatFunction<?> f) {
         NetworkHelper.writeItemPredicate(buf, ((EquipmentCountFunction)f).item);
      }

      public FloatFunction<?> createDefaultInstance() {
         return new EquipmentCountFunction(this.kind, this.self, NoneItemStackPredicate.INSTANCE);
      }
   }
}
