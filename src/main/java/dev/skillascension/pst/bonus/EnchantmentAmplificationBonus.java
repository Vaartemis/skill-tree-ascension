package dev.skillascension.pst.bonus;

import com.google.gson.JsonObject;
import dev.skillascension.pst.AscensionBonuses;
import daripher.skilltree.client.tooltip.TooltipHelper;
import daripher.skilltree.client.widget.editor.SkillTreeEditor;
import daripher.skilltree.skill.bonus.SkillBonus;
import daripher.skilltree.skill.bonus.SkillBonus.Target;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class EnchantmentAmplificationBonus implements SkillBonus<EnchantmentAmplificationBonus> {
   private float chance;
   private TableBonusData data;

   public EnchantmentAmplificationBonus(float chance) {
      this(chance, new TableBonusData());
   }

   EnchantmentAmplificationBonus(float chance, TableBonusData data) {
      this.chance = chance;
      this.data = data;
   }

   public float getChance() {
      return this.chance;
   }

   public float valueFor(Player player, ItemStack stack) {
      return stack != null && !this.data.item.test(stack) ? 0.0F : this.chance * this.data.multiplier.getValue(player);
   }

   public daripher.skilltree.skill.bonus.SkillBonus.Serializer getSerializer() {
      return (daripher.skilltree.skill.bonus.SkillBonus.Serializer)AscensionBonuses.ENCHANTMENT_AMPLIFICATION.get();
   }

   public EnchantmentAmplificationBonus copy() {
      return new EnchantmentAmplificationBonus(this.chance, this.data.copy());
   }

   public EnchantmentAmplificationBonus multiply(double multiplier) {
      this.chance = (float)((double)this.chance * multiplier);
      return this;
   }

   public boolean canMerge(SkillBonus<?> other) {
      if (other instanceof EnchantmentAmplificationBonus o && o.data.sameAs(this.data)) {
         return true;
      }

      return false;
   }

   public SkillBonus<EnchantmentAmplificationBonus> merge(SkillBonus<?> other) {
      return new EnchantmentAmplificationBonus(this.chance + ((EnchantmentAmplificationBonus)other).chance, this.data.copy());
   }

   public MutableComponent getSimpleTooltip() {
      MutableComponent c = Component.translatable(
         "skillascension.skill_bonus.enchantment_amplification",
         new Object[]{TooltipHelper.formatNumber((double)(this.chance * 100.0F)), this.data.item.getTooltip("plural")}
      );
      c = this.data.multiplier.getTooltip(c, Target.PLAYER);
      return c.withStyle(TooltipHelper.getSkillBonusStyle(this.isPositive()));
   }

   public boolean isPositive() {
      return this.chance > 0.0F;
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<EnchantmentAmplificationBonus> consumer) {
      editor.addLabel(0, 0, "Chance", new ChatFormatting[]{ChatFormatting.GOLD});
      editor.increaseHeight(19);
      editor.addNumericTextField(0, 0, 50, 14, (double)this.chance).setNumericResponder(value -> {
         this.chance = value.floatValue();
         consumer.accept(this.copy());
      });
      editor.increaseHeight(19);
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.SkillBonus.Serializer {
      public SkillBonus<?> deserialize(JsonObject json) {
         return new EnchantmentAmplificationBonus(json.has("chance") ? json.get("chance").getAsFloat() : 0.1F, TableBonusData.read(json));
      }

      public void serialize(JsonObject json, SkillBonus<?> bonus) {
         EnchantmentAmplificationBonus b = (EnchantmentAmplificationBonus)bonus;
         json.addProperty("chance", b.chance);
         b.data.write(json);
      }

      public SkillBonus<?> deserialize(CompoundTag tag) {
         return new EnchantmentAmplificationBonus(tag.getFloat("chance"), TableBonusData.read(tag));
      }

      public CompoundTag serialize(SkillBonus<?> bonus) {
         EnchantmentAmplificationBonus b = (EnchantmentAmplificationBonus)bonus;
         CompoundTag tag = new CompoundTag();
         tag.putFloat("chance", b.chance);
         b.data.write(tag);
         return tag;
      }

      public SkillBonus<?> deserialize(FriendlyByteBuf buf) {
         return this.deserialize(buf.readNbt());
      }

      public void serialize(FriendlyByteBuf buf, SkillBonus<?> bonus) {
         buf.writeNbt(this.serialize(bonus));
      }

      public SkillBonus<?> createDefaultInstance() {
         return new EnchantmentAmplificationBonus(0.1F);
      }
   }
}
