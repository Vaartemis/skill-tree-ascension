package dev.skillascension.stacks;

import dev.skillascension.pets.PetEvents;
import daripher.skilltree.init.PSTFloatFunctions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.RegisterEvent;

@EventBusSubscriber(
   modid = "skillascension",
   bus = Bus.MOD
)
public final class StacksRegistration {
   public static final StacksFunction.Serializer STACKS = new StacksFunction.Serializer(() -> StacksRegistration.STACKS);

   private StacksRegistration() {
   }

   @SubscribeEvent
   public static void onSetup(FMLCommonSetupEvent var0) {
      Counters.hookSpells();
      PetEvents.init();
   }

   @SubscribeEvent
   public static void onRegister(RegisterEvent var0) {
      ResourceKey var1 = ResourceKey.createRegistryKey(PSTFloatFunctions.REGISTRY_ID);
      var0.register(var1, var0x -> {
         var0x.register(new ResourceLocation("skillascension", "stacks"), STACKS);
         StacksLog.LOG.info("Registered PST value provider skillascension:stacks");
      });
      PetEvents.onRegister(var0);
   }
}
