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

public final class XpAegisBonus implements SkillBonus<XpAegisBonus> {
   private float amount;
   private TableBonusData data;

   public XpAegisBonus(float amount) {
      this(amount, new TableBonusData());
   }

   XpAegisBonus(float amount, TableBonusData data) {
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
      return (daripher.skilltree.skill.bonus.SkillBonus.Serializer)AscensionBonuses.XP_AEGIS.get();
   }

   public XpAegisBonus copy() {
      return new XpAegisBonus(this.amount, this.data.copy());
   }

   public XpAegisBonus multiply(double multiplier) {
      this.amount = (float)((double)this.amount * multiplier);
      return this;
   }

   public boolean canMerge(SkillBonus<?> other) {
      if (other instanceof XpAegisBonus o && o.data.sameAs(this.data)) {
         return true;
      }

      return false;
   }

   public SkillBonus<XpAegisBonus> merge(SkillBonus<?> other) {
      return new XpAegisBonus(this.amount + ((XpAegisBonus)other).amount, this.data.copy());
   }

   public MutableComponent getSimpleTooltip() {
      MutableComponent c = Component.translatable(
         "skillascension.skill_bonus.xp_aegis", new Object[]{TooltipHelper.formatNumber((double)(this.amount * 100.0F)), this.data.item.getTooltip("plural")}
      );
      c = this.data.multiplier.getTooltip(c, Target.PLAYER);
      return c.withStyle(TooltipHelper.getSkillBonusStyle(this.isPositive()));
   }

   public boolean isPositive() {
      return this.amount > 0.0F;
   }

   public void addEditorWidgets(SkillTreeEditor editor, Consumer<XpAegisBonus> consumer) {
      editor.addLabel(0, 0, "Absorbed", new ChatFormatting[]{ChatFormatting.GOLD});
      editor.increaseHeight(19);
      editor.addNumericTextField(0, 0, 50, 14, (double)this.amount).setNumericResponder(value -> {
         this.amount = value.floatValue();
         consumer.accept(this.copy());
      });
      editor.increaseHeight(19);
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.SkillBonus.Serializer {
      public SkillBonus<?> deserialize(JsonObject json) {
         return new XpAegisBonus(json.has("amount") ? json.get("amount").getAsFloat() : 0.1F, TableBonusData.read(json));
      }

      public void serialize(JsonObject json, SkillBonus<?> bonus) {
         XpAegisBonus b = (XpAegisBonus)bonus;
         json.addProperty("amount", b.amount);
         b.data.write(json);
      }

      public SkillBonus<?> deserialize(CompoundTag tag) {
         return new XpAegisBonus(tag.getFloat("amount"), TableBonusData.read(tag));
      }

      public CompoundTag serialize(SkillBonus<?> bonus) {
         XpAegisBonus b = (XpAegisBonus)bonus;
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
         return new XpAegisBonus(0.1F);
      }
   }
}
