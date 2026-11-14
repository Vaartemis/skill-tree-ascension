package dev.skillascension.pst;

import dev.skillascension.pst.bonus.BonusEnchantmentBonus;
import dev.skillascension.pst.bonus.EnchantingDiscountBonus;
import dev.skillascension.pst.bonus.EnchantmentAmplificationBonus;
import dev.skillascension.pst.bonus.EventBonus;
import dev.skillascension.pst.bonus.GemPowerBonus;
import dev.skillascension.pst.bonus.XpAegisBonus;
import dev.skillascension.pst.bonus.XpSiphonBonus;
import dev.skillascension.pst.function.EquipmentCountFunction;
import dev.skillascension.pst.function.ExperienceLevelFunction;
import dev.skillascension.pst.function.SpellPowerSumFunction;
import daripher.skilltree.init.PSTFloatFunctions;
import daripher.skilltree.init.PSTSkillBonuses;
import daripher.skilltree.skill.bonus.SkillBonus.Serializer;
import java.util.EnumMap;
import java.util.Map;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class AscensionBonuses {
   private static final DeferredRegister<Serializer> REGISTRY = DeferredRegister.create(PSTSkillBonuses.REGISTRY_ID, "skillascension");
   public static final RegistryObject<Serializer> ENCHANTMENT_AMPLIFICATION = REGISTRY.register(
      "enchantment_amplification", EnchantmentAmplificationBonus.Serializer::new
   );
   public static final RegistryObject<Serializer> ENCHANTING_DISCOUNT = REGISTRY.register("enchanting_discount", EnchantingDiscountBonus.Serializer::new);
   public static final RegistryObject<Serializer> BONUS_ENCHANTMENT = REGISTRY.register("bonus_enchantment", BonusEnchantmentBonus.Serializer::new);
   public static final RegistryObject<Serializer> GEM_POWER = REGISTRY.register("gem_power", GemPowerBonus.Serializer::new);
   public static final RegistryObject<Serializer> XP_AEGIS = REGISTRY.register("xp_aegis", XpAegisBonus.Serializer::new);
   public static final RegistryObject<Serializer> XP_SIPHON = REGISTRY.register("xp_siphon", XpSiphonBonus.Serializer::new);
   private static final DeferredRegister<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> FUNCTIONS = DeferredRegister.create(
      PSTFloatFunctions.REGISTRY_ID, "skillascension"
   );
   public static RegistryObject<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> GEMS_AMOUNT = FUNCTIONS.register(
      "gems_amount",
      () -> new EquipmentCountFunction.Serializer(
            EquipmentCountFunction.Kind.GEMS, () -> (daripher.skilltree.skill.bonus.function.FloatFunction.Serializer)AscensionBonuses.GEMS_AMOUNT.get()
         )
   );
   public static RegistryObject<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> CURSE_AMOUNT = FUNCTIONS.register(
      "curse_amount",
      () -> new EquipmentCountFunction.Serializer(
            EquipmentCountFunction.Kind.CURSES, () -> (daripher.skilltree.skill.bonus.function.FloatFunction.Serializer)AscensionBonuses.CURSE_AMOUNT.get()
         )
   );
   public static RegistryObject<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> EXPERIENCE_LEVEL = FUNCTIONS.register(
      "experience_level",
      () -> new ExperienceLevelFunction.Serializer(() -> (daripher.skilltree.skill.bonus.function.FloatFunction.Serializer)AscensionBonuses.EXPERIENCE_LEVEL.get())
   );
   public static RegistryObject<daripher.skilltree.skill.bonus.function.FloatFunction.Serializer> SPELL_POWER_SUM = FUNCTIONS.register(
      "spell_power_sum",
      () -> new SpellPowerSumFunction.Serializer(() -> (daripher.skilltree.skill.bonus.function.FloatFunction.Serializer)AscensionBonuses.SPELL_POWER_SUM.get())
   );
   public static RegistryObject<Serializer> XP_INTEREST = REGISTRY.register(
      "xp_interest", () -> new EventBonus.Serializer(EventBonus.Kind.XP_INTEREST, () -> AscensionBonuses.XP_INTEREST)
   );
   public static RegistryObject<Serializer> LEVEL_SURGE = REGISTRY.register(
      "level_surge", () -> new EventBonus.Serializer(EventBonus.Kind.LEVEL_SURGE, () -> AscensionBonuses.LEVEL_SURGE)
   );
   public static RegistryObject<Serializer> LAST_PAGE = REGISTRY.register(
      "last_page", () -> new EventBonus.Serializer(EventBonus.Kind.LAST_PAGE, () -> AscensionBonuses.LAST_PAGE)
   );
   public static RegistryObject<Serializer> LEARNING_BLADE = REGISTRY.register(
      "learning_blade", () -> new EventBonus.Serializer(EventBonus.Kind.LEARNING_BLADE, () -> AscensionBonuses.LEARNING_BLADE)
   );
   public static final Map<EventBonus.Kind, RegistryObject<Serializer>> CLASS_EVENTS = new EnumMap<>(EventBonus.Kind.class);

   private AscensionBonuses() {
   }

   public static void register(IEventBus modBus) {
      REGISTRY.register(modBus);
      FUNCTIONS.register(modBus);
   }

   static {
      for (EventBonus.Kind k : EventBonus.Kind.values()) {
         if (k.ordinal() > EventBonus.Kind.LEARNING_BLADE.ordinal()) {
            CLASS_EVENTS.put(k, REGISTRY.register(k.id, () -> new EventBonus.Serializer(k, () -> CLASS_EVENTS.get(k))));
         }
      }
   }
}
