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

public final class XpSiphonBonus implements SkillBonus<XpSiphonBonus> {
   private float amount;
   private TableBonusData data;

   public XpSiphonBonus(float amount) {
      this(amount, new TableBonusData());
   }

   XpSiphonBonus(float amount, TableBonusData data) {
      this.amount = amount;
      this.data = data;
   }

   public float getAmount() {
      return this.amount;
   }

   public float valueFor(Player player, ItemStack stack) {
      return stack != null && !this.data.item.test(stack) ? 0.0F : this.amount * this.data.multiplier.getValue(player);
   }

   public daripher.skilltree.skill.bonus.SkillBonus.Serializer getSerializer() {
      return (daripher.skilltree.skill.bonus.SkillBonus.Serializer)AscensionBonuses.XP_SIPHON.get();
   }

   public XpSiphonBonus copy() {
      return new XpSiphonBonus(this.amount, this.data.copy());
   }

   public XpSiphonBonus multiply(double multiplier) {
      this.amount = (float)((double)this.amount * multiplier);
      return this;
   }

   public boolean canMerge(SkillBonus<?> other) {
      if (other instanceof XpSiphonBonus o && o.data.sameAs(this.data)) {
         return true;
      }

      return false;
   }

   public SkillBonus<XpSiphonBonus> merge(SkillBonus<?> other) {
      return new XpSiphonBonus(this.amount + ((XpSiphonBonus)other).amount, this.data.copy());
   }

   public MutableComponent getSimpleTooltip() {
      MutableComponent c = Component.translatable(
         "skillascension.skill_bonus.xp_siphon", new Object[]{TooltipHelper.formatNumber((double)(this.amount * 100.0F)), this.data.item.getTooltip("plural")}
      );
      c = this.data.multiplier.getTooltip(c, Target.PLAYER);
      return c.withStyle(TooltipHelper.getSkillBonusStyle(this.isPositive()));
   }

   public boolean isPositive() {
      return this.amount > 0.0F;
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<XpSiphonBonus> consumer) {
      editor.addLabel(0, 0, "Healing", new ChatFormatting[]{ChatFormatting.GOLD});
      editor.increaseHeight(19);
      editor.addNumericTextField(0, 0, 50, 14, (double)this.amount).setNumericResponder(value -> {
         this.amount = value.floatValue();
         consumer.accept(this.copy());
      });
      editor.increaseHeight(19);
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.SkillBonus.Serializer {
      public SkillBonus<?> deserialize(JsonObject json) {
         return new XpSiphonBonus(json.has("amount") ? json.get("amount").getAsFloat() : 0.1F, TableBonusData.read(json));
      }

      public void serialize(JsonObject json, SkillBonus<?> bonus) {
         XpSiphonBonus b = (XpSiphonBonus)bonus;
         json.addProperty("amount", b.amount);
         b.data.write(json);
      }

      public SkillBonus<?> deserialize(CompoundTag tag) {
         return new XpSiphonBonus(tag.getFloat("amount"), TableBonusData.read(tag));
      }

      public CompoundTag serialize(SkillBonus<?> bonus) {
         XpSiphonBonus b = (XpSiphonBonus)bonus;
         CompoundTag tag = new CompoundTag();
         tag.putFloat("amount", b.amount);
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
         return new XpSiphonBonus(0.1F);
      }
   }
}
