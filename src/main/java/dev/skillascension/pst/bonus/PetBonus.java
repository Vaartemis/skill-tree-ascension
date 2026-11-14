package dev.skillascension.pst.bonus;

import com.google.gson.JsonObject;
import daripher.skilltree.client.tooltip.TooltipHelper;
import daripher.skilltree.client.widget.editor.SkillTreeEditor;
import daripher.skilltree.skill.bonus.SkillBonus;
import daripher.skilltree.skill.bonus.SkillBonus.Target;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;

public final class PetBonus implements SkillBonus<PetBonus> {
   private final PetBonus.Kind kind;
   private final Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> serializer;
   private float amount;
   private float b;
   private float c;
   private TableBonusData data;

   PetBonus(PetBonus.Kind var1, Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> var2, float var3, float var4, float var5, TableBonusData var6) {
      this.kind = var1;
      this.serializer = var2;
      this.amount = var3;
      this.b = var4;
      this.c = var5;
      this.data = var6;
   }

   public PetBonus.Kind kind() {
      return this.kind;
   }

   public float amount(Player var1) {
      return this.amount * this.data.multiplier.getValue(var1);
   }

   public float b() {
      return this.b;
   }

   public float c() {
      return this.c;
   }

   public daripher.skilltree.skill.bonus.SkillBonus.Serializer getSerializer() {
      return this.serializer.get();
   }

   public PetBonus copy() {
      return new PetBonus(this.kind, this.serializer, this.amount, this.b, this.c, this.data.copy());
   }

   public PetBonus multiply(double var1) {
      this.amount = (float)((double)this.amount * var1);
      return this;
   }

   public boolean canMerge(SkillBonus<?> var1) {
      if (var1 instanceof PetBonus var2 && var2.kind == this.kind && var2.data.sameAs(this.data)) {
         return true;
      }

      return false;
   }

   public SkillBonus<PetBonus> merge(SkillBonus<?> var1) {
      PetBonus var2 = (PetBonus)var1;
      return new PetBonus(
         this.kind,
         this.serializer,
         this.amount + var2.amount,
         this.kind.minB() ? Math.min(this.b, var2.b) : Math.max(this.b, var2.b),
         Math.max(this.c, var2.c),
         this.data.copy()
      );
   }

   public MutableComponent getSimpleTooltip() {
      String var1 = this.kind.raw ? TooltipHelper.formatNumber((double)this.amount) : TooltipHelper.formatNumber((double)(this.amount * 100.0F));
      MutableComponent var2 = Component.translatable(
         "skillascension.skill_bonus." + this.kind.id, new Object[]{var1, TooltipHelper.formatNumber((double)this.b), TooltipHelper.formatNumber((double)this.c)}
      );
      var2 = this.data.multiplier.getTooltip(var2, Target.PLAYER);
      return var2.withStyle(TooltipHelper.getSkillBonusStyle(this.isPositive()));
   }

   public boolean isPositive() {
      return this.amount > 0.0F;
   }

   public void addEditorWidgets(SkillTreeEditor var1, Consumer<PetBonus> var2) {
      var1.addLabel(0, 0, "Amount", new ChatFormatting[]{ChatFormatting.GOLD});
      var1.increaseHeight(19);
      var1.addNumericTextField(0, 0, 50, 14, (double)this.amount).setNumericResponder(var2x -> {
         this.amount = var2x.floatValue();
         var2.accept(this.copy());
      });
      var1.increaseHeight(19);
   }

   public static enum Kind {
      PACK_LEADER("pack_leader", "radius", 16.0F, "max_pets", 5.0F, false),
      PACK_HIDE("pack_hide", "radius", 16.0F, "max_pets", 5.0F, false),
      FERAL_BOND("feral_bond", "", 0.0F, "", 0.0F, false),
      BLOOD_PACT("blood_pact", "", 0.0F, "", 0.0F, false),
      DEN("den", "interval", 2.0F, "radius", 24.0F, false),
      PREY("prey", "duration", 6.0F, "", 0.0F, false),
      PINCER("pincer", "duration", 3.0F, "", 0.0F, false),
      FRENZY("frenzy", "max_stacks", 10.0F, "window", 8.0F, false),
      GUARDIAN("guardian", "threshold", 35.0F, "cooldown", 30.0F, false),
      BEAST_SHIELD("beast_shield", "radius", 16.0F, "", 0.0F, false),
      VENGEANCE("vengeance", "radius", 32.0F, "duration", 5.0F, false),
      SAVAGE("savage", "multiplier", 2.0F, "", 0.0F, false),
      SPIKED_COLLAR("spiked_collar", "", 0.0F, "", 0.0F, false),
      HOWL("howl", "interval", 20.0F, "duration", 6.0F, true),
      UNDYING("undying", "cooldown", 60.0F, "duration", 6.0F, false);

      public final String id;
      final String keyB;
      final String keyC;
      final float defB;
      final float defC;
      final boolean raw;

      private Kind(String nullxx, String nullxxx, float nullxxxx, String nullxxxxx, float nullxxxxxx, boolean nullxxxxxxx) {
         this.id = nullxx;
         this.keyB = nullxxx;
         this.defB = nullxxxx;
         this.keyC = nullxxxxx;
         this.defC = nullxxxxxx;
         this.raw = nullxxxxxxx;
      }

      public boolean minB() {
         return this.keyB.equals("interval");
      }
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.SkillBonus.Serializer {
      private final PetBonus.Kind kind;
      private final Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> self;

      public Serializer(PetBonus.Kind var1, Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> var2) {
         this.kind = var1;
         this.self = var2;
      }

      private float get(JsonObject var1, String var2, float var3) {
         return !var2.isEmpty() && var1.has(var2) ? var1.get(var2).getAsFloat() : var3;
      }

      public SkillBonus<?> deserialize(JsonObject var1) {
         return new PetBonus(
            this.kind,
            this.self,
            this.get(var1, "amount", 0.0F),
            this.get(var1, this.kind.keyB, this.kind.defB),
            this.get(var1, this.kind.keyC, this.kind.defC),
            TableBonusData.read(var1)
         );
      }

      public void serialize(JsonObject var1, SkillBonus<?> var2) {
         PetBonus var3 = (PetBonus)var2;
         var1.addProperty("amount", var3.amount);
         if (!this.kind.keyB.isEmpty()) {
            var1.addProperty(this.kind.keyB, var3.b);
         }

         if (!this.kind.keyC.isEmpty()) {
            var1.addProperty(this.kind.keyC, var3.c);
         }

         var3.data.write(var1);
      }

      public SkillBonus<?> deserialize(CompoundTag var1) {
         return new PetBonus(this.kind, this.self, var1.getFloat("amount"), var1.getFloat("b"), var1.getFloat("c"), TableBonusData.read(var1));
      }

      public CompoundTag serialize(SkillBonus<?> var1) {
         PetBonus var2 = (PetBonus)var1;
         CompoundTag var3 = new CompoundTag();
         var3.putFloat("amount", var2.amount);
         var3.putFloat("b", var2.b);
         var3.putFloat("c", var2.c);
         var2.data.write(var3);
         return var3;
      }

      public SkillBonus<?> deserialize(FriendlyByteBuf var1) {
         return this.deserialize(var1.readNbt());
      }

      public void serialize(FriendlyByteBuf var1, SkillBonus<?> var2) {
         var1.writeNbt(this.serialize(var2));
      }

      public SkillBonus<?> createDefaultInstance() {
         return new PetBonus(this.kind, this.self, 0.1F, this.kind.defB, this.kind.defC, new TableBonusData());
      }
   }
}
