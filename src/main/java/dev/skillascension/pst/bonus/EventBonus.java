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
import net.minecraftforge.registries.RegistryObject;

public final class EventBonus implements SkillBonus<EventBonus> {
   private final EventBonus.Kind kind;
   private final Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> serializer;
   private float amount;
   private float b;
   private float c;
   private TableBonusData data;

   EventBonus(
      EventBonus.Kind var1, Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> var2, float var3, float var4, float var5, TableBonusData var6
   ) {
      this.kind = var1;
      this.serializer = var2;
      this.amount = var3;
      this.b = var4;
      this.c = var5;
      this.data = var6;
   }

   public EventBonus.Kind kind() {
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

   public EventBonus copy() {
      return new EventBonus(this.kind, this.serializer, this.amount, this.b, this.c, this.data.copy());
   }

   public EventBonus multiply(double var1) {
      this.amount = (float)((double)this.amount * var1);
      return this;
   }

   public boolean canMerge(SkillBonus<?> var1) {
      if (var1 instanceof EventBonus var2 && var2.kind == this.kind && var2.data.sameAs(this.data)) {
         return true;
      }

      return false;
   }

   public SkillBonus<EventBonus> merge(SkillBonus<?> var1) {
      EventBonus var2 = (EventBonus)var1;
      float var3 = this.kind != EventBonus.Kind.XP_INTEREST && this.kind != EventBonus.Kind.LAST_PAGE ? Math.max(this.b, var2.b) : Math.min(this.b, var2.b);
      float var4 = this.kind != EventBonus.Kind.LAST_PAGE && this.kind != EventBonus.Kind.HIGH_TIDE ? Math.max(this.c, var2.c) : Math.min(this.c, var2.c);
      return new EventBonus(this.kind, this.serializer, this.amount + var2.amount, var3, var4, this.data.copy());
   }

   public MutableComponent getSimpleTooltip() {
      boolean var1 = this.kind == EventBonus.Kind.XP_INTEREST
         || this.kind == EventBonus.Kind.LIVING_FORGE
         || this.kind == EventBonus.Kind.ANVIL
         || this.kind == EventBonus.Kind.SHELL
         || this.kind == EventBonus.Kind.CATCH_XP;
      String var2 = var1 ? TooltipHelper.formatNumber((double)this.amount) : TooltipHelper.formatNumber((double)(this.amount * 100.0F));
      MutableComponent var3 = Component.translatable(
         "skillascension.skill_bonus." + this.kind.id, new Object[]{var2, TooltipHelper.formatNumber((double)this.b), TooltipHelper.formatNumber((double)this.c)}
      );
      var3 = this.data.multiplier.getTooltip(var3, Target.PLAYER);
      return var3.withStyle(TooltipHelper.getSkillBonusStyle(this.isPositive()));
   }

   public boolean isPositive() {
      return this.amount > 0.0F;
   }

   public void addEditorWidgets(SkillTreeEditor var1, Consumer<EventBonus> var2) {
      var1.addLabel(0, 0, "Amount", new ChatFormatting[]{ChatFormatting.GOLD});
      var1.increaseHeight(19);
      var1.addNumericTextField(0, 0, 50, 14, (double)this.amount).setNumericResponder(var2x -> {
         this.amount = var2x.floatValue();
         var2.accept(this.copy());
      });
      var1.increaseHeight(19);
   }

   public static enum Kind {
      XP_INTEREST("xp_interest", "interval", 30.0F, "", 0.0F),
      LEVEL_SURGE("level_surge", "absorption", 8.0F, "duration", 20.0F),
      LAST_PAGE("last_page", "cost", 10.0F, "cooldown", 180.0F),
      LEARNING_BLADE("learning_blade", "over_max", 2.0F, "", 0.0F),
      FURY("fury", "max_stacks", 10.0F, "decay", 3.0F),
      HUNTERS_MARK("hunters_mark", "duration", 8.0F, "", 0.0F),
      LIVING_FORGE("living_forge", "", 0.0F, "", 0.0F),
      ANVIL("anvil", "max_stacks", 10.0F, "duration", 5.0F),
      FEAST("feast", "radius", 8.0F, "", 0.0F),
      SHARED_POTION("shared_potion", "radius", 8.0F, "", 0.0F),
      ARCANE_ECHO("arcane_echo", "", 0.0F, "", 0.0F),
      HIGH_TIDE("high_tide", "max_stacks", 5.0F, "interval", 10.0F),
      BOND("bond", "radius", 16.0F, "", 0.0F),
      HARPOON("harpoon", "pull", 0.6F, "", 0.0F),
      STORMCALLER("stormcaller", "damage", 6.0F, "", 0.0F),
      SHELL("shell", "max", 10.0F, "", 0.0F),
      CATCH_XP("catch_xp", "", 0.0F, "", 0.0F),
      MANA_FLOW("mana_flow", "mana", 100.0F, "duration", 6.0F),
      OVERCHANNEL("overchannel", "threshold", 25.0F, "", 0.0F),
      SCHOOL_CHAIN("school_chain", "max_stacks", 5.0F, "window", 6.0F),
      COUNTER("counter", "window", 3.0F, "", 0.0F),
      EXECUTE("execute", "threshold", 25.0F, "", 0.0F),
      KILL_STREAK("kill_streak", "max_stacks", 10.0F, "window", 8.0F),
      OVERHEAL("overheal", "max", 10.0F, "", 0.0F),
      WAR_CRY("war_cry", "threshold", 50.0F, "cooldown", 60.0F),
      MEMENTO_MORI("memento_mori", "", 0.0F, "", 0.0F),
      SHADOWSTEP("shadowstep", "", 0.0F, "", 0.0F),
      SHADOWVEIL("shadowveil", "", 0.0F, "", 0.0F),
      NIGHT_VIGIL("night_vigil", "", 0.0F, "", 0.0F),
      RUNE_SCRIBE("rune_scribe", "", 0.0F, "", 0.0F),
      WITCH_DOCTOR("witch_doctor", "", 0.0F, "", 0.0F),
      TREASURE_SEEKER("treasure_seeker", "", 0.0F, "", 0.0F);

      public final String id;
      final String keyB;
      final String keyC;
      final float defB;
      final float defC;

      private Kind(String nullxx, String nullxxx, float nullxxxx, String nullxxxxx, float nullxxxxxx) {
         this.id = nullxx;
         this.keyB = nullxxx;
         this.defB = nullxxxx;
         this.keyC = nullxxxxx;
         this.defC = nullxxxxxx;
      }
   }

   public static final class Serializer implements daripher.skilltree.skill.bonus.SkillBonus.Serializer {
      private final EventBonus.Kind kind;
      private final Supplier<daripher.skilltree.skill.bonus.SkillBonus.Serializer> self;

      public Serializer(EventBonus.Kind var1, Supplier<RegistryObject<daripher.skilltree.skill.bonus.SkillBonus.Serializer>> var2) {
         this.kind = var1;
         this.self = () -> (daripher.skilltree.skill.bonus.SkillBonus.Serializer)((RegistryObject)var2.get()).get();
      }

      private float get(JsonObject var1, String var2, float var3) {
         return !var2.isEmpty() && var1.has(var2) ? var1.get(var2).getAsFloat() : var3;
      }

      public SkillBonus<?> deserialize(JsonObject var1) {
         return new EventBonus(
            this.kind,
            this.self,
            this.get(var1, "amount", 0.0F),
            this.get(var1, this.kind.keyB, this.kind.defB),
            this.get(var1, this.kind.keyC, this.kind.defC),
            TableBonusData.read(var1)
         );
      }

      public void serialize(JsonObject var1, SkillBonus<?> var2) {
         EventBonus var3 = (EventBonus)var2;
         var1.addProperty("amount", var3.amount);
         var1.addProperty(this.kind.keyB, var3.b);
         if (!this.kind.keyC.isEmpty()) {
            var1.addProperty(this.kind.keyC, var3.c);
         }

         var3.data.write(var1);
      }

      public SkillBonus<?> deserialize(CompoundTag var1) {
         return new EventBonus(this.kind, this.self, var1.getFloat("amount"), var1.getFloat("b"), var1.getFloat("c"), TableBonusData.read(var1));
      }

      public CompoundTag serialize(SkillBonus<?> var1) {
         EventBonus var2 = (EventBonus)var1;
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
         return new EventBonus(this.kind, this.self, 0.1F, this.kind.defB, this.kind.defC, new TableBonusData());
      }
   }
}
