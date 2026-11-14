package dev.skillascension.mixin;

import dev.skillascension.pst.PstServerSkills;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
   targets = {"daripher.skilltree.network.message.GainSkillPointMessage"},
   remap = false
)
public abstract class PstGainSkillPointMainThreadMixin {
   @Inject(
      method = {"receive"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false,
      require = 0
   )
   private static void skillascension$gainOnServerThread(Supplier<Context> ctxSupplier, CallbackInfo ci) {
      Context ctx = ctxSupplier.get();
      ctx.setPacketHandled(true);
      ServerPlayer player = ctx.getSender();
      ci.cancel();
      if (player != null) {
         ctx.enqueueWork(() -> PstServerSkills.gainSkillPoint(player));
      }
   }
}
