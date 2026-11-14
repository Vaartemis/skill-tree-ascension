package dev.skillascension.mixin;

import daripher.skilltree.network.message.LearnSkillMessage;
import dev.skillascension.pst.PstServerSkills;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/** Learning a skill runs on the server thread, with point types, costs and repeatable levels. */
@Pseudo
@Mixin(targets = "daripher.skilltree.network.message.LearnSkillMessage", remap = false)
public abstract class PstLearnSkillMainThreadMixin {
    @Inject(method = "receive", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void skillascension$learnOnServerThread(LearnSkillMessage message, Supplier<NetworkEvent.Context> ctxSupplier, CallbackInfo ci) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);
        ServerPlayer player = ctx.getSender();
        ResourceLocation id = ((LearnSkillMessageAccessor) (Object) message).skillascension$skillId();
        ci.cancel();
        if (player != null && id != null) ctx.enqueueWork(() -> PstServerSkills.learn(player, id));
    }
}
