package dev.skillascension.mixin;

import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = "daripher.skilltree.network.message.LearnSkillMessage", remap = false)
public interface LearnSkillMessageAccessor {
    @Accessor(value = "skillId", remap = false)
    ResourceLocation skillascension$skillId();
}
