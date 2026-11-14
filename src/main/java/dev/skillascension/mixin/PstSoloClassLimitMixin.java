package dev.skillascension.mixin;

import dev.skillascension.common.Limits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/** Client: the singleplayer limitation bonus from the config (the server checks the same rule). */
@Pseudo
@Mixin(targets = {"daripher.skilltree.client.widget.SkillTreeWidgets", "daripher.skilltree.client.widget.skill.SkillButton"}, remap = false)
public abstract class PstSoloClassLimitMixin {
    @Redirect(method = {"canLearnSkill", "addLimitationsTooltip"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            remap = false, require = 0)
    private Object skillascension$soloLimit(Map<Object, Object> limits, Object tag, Object fallback) {
        Object limit = limits.getOrDefault(tag, fallback);
        if (tag instanceof String name && limit instanceof Integer value) {
            IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            return Limits.limit(name, value, server != null && !server.isPublished());
        }
        return limit;
    }
}
