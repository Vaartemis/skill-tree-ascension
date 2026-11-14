package dev.skillascension.pst.bonus;

import com.google.gson.JsonObject;
import daripher.skilltree.data.serializers.SerializationHelper;
import daripher.skilltree.skill.bonus.multiplier.LivingMultiplier;
import daripher.skilltree.skill.bonus.multiplier.NoneLivingMultiplier;
import daripher.skilltree.skill.bonus.predicate.item.ItemStackPredicate;
import daripher.skilltree.skill.bonus.predicate.item.NoneItemStackPredicate;
import net.minecraft.nbt.CompoundTag;

final class TableBonusData {
   ItemStackPredicate item = NoneItemStackPredicate.INSTANCE;
   LivingMultiplier multiplier = NoneLivingMultiplier.INSTANCE;

   TableBonusData copy() {
      TableBonusData d = new TableBonusData();
      d.item = this.item;
      d.multiplier = this.multiplier;
      return d;
   }

   boolean sameAs(TableBonusData o) {
      return this.item.equals(o.item) && this.multiplier.equals(o.multiplier);
   }

   static TableBonusData read(JsonObject json) {
      TableBonusData d = new TableBonusData();
      if (json.has("item_condition")) {
         d.item = SerializationHelper.deserializeItemPredicate(json, "item_condition");
      }

      if (json.has("player_multiplier")) {
         d.multiplier = SerializationHelper.deserializeLivingMultiplier(json, "player_multiplier");
      }

      return d;
   }

   void write(JsonObject json) {
      SerializationHelper.serializeItemPredicate(json, this.item, "item_condition");
      SerializationHelper.serializeLivingMultiplier(json, this.multiplier, "player_multiplier");
   }

   static TableBonusData read(CompoundTag tag) {
      TableBonusData d = new TableBonusData();
      if (tag.contains("item_condition")) {
         d.item = SerializationHelper.deserializeItemPredicate(tag);
      }

      if (tag.contains("player_multiplier")) {
         d.multiplier = SerializationHelper.deserializeLivingMultiplier(tag, "player_multiplier");
      }

      return d;
   }

   void write(CompoundTag tag) {
      SerializationHelper.serializeItemPredicate(tag, this.item);
      SerializationHelper.serializeLivingMultiplier(tag, this.multiplier, "player_multiplier");
   }
}
