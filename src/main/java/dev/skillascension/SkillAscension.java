package dev.skillascension;

import dev.skillascension.client.PstCooldownHud;
import dev.skillascension.client.PstPointBadge;
import dev.skillascension.compat.Compat;
import dev.skillascension.data.AscensionData;
import dev.skillascension.net.AscensionNet;
import dev.skillascension.points.PointSources;
import dev.skillascension.points.PointsCommand;
import dev.skillascension.pst.AscensionBonuses;
import dev.skillascension.pst.ClassEvents;
import dev.skillascension.pst.CombatEvents;
import dev.skillascension.pst.DeathKnowledgeEvents;
import dev.skillascension.pst.RepeatableSkills;
import dev.skillascension.pst.XpEvents;
import dev.skillascension.pst.XpGuardEvents;
import dev.skillascension.stacks.ClassCounters;
import dev.skillascension.stacks.Counters;
import dev.skillascension.stacks.DeedFilter;
import dev.skillascension.stacks.EternalStacks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Skill Tree Ascension: an addon for Passive Skill Tree. */
@Mod(SkillAscension.MODID)
public final class SkillAscension {
    public static final String MODID = "skillascension";
    public static final Logger LOG = LogManager.getLogger("Skill Tree Ascension");

    public SkillAscension() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, AscensionConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AscensionConfig.CLIENT_SPEC);
        AscensionBonuses.register(modBus);
        AscensionNet.register();

        IEventBus forge = MinecraftForge.EVENT_BUS;
        forge.addListener(AscensionData::onAddReloadListeners);
        forge.addListener(AscensionData::onDatapackSync);
        forge.register(RepeatableSkills.class);
        forge.register(PointSources.class);
        forge.register(PointsCommand.class);
        forge.register(dev.skillascension.points.TreeAccessEvents.class);
        forge.register(dev.skillascension.gates.GateEvents.class);
        forge.register(ClassEvents.class);
        forge.register(CombatEvents.class);
        forge.register(DeathKnowledgeEvents.class);
        forge.register(XpEvents.class);
        forge.register(XpGuardEvents.class);
        forge.register(ClassCounters.class);
        forge.register(Counters.class);
        forge.register(DeedFilter.class);
        forge.register(EternalStacks.class);
        if (Compat.IRONS) {
            forge.register(dev.skillascension.compat.irons.IronsCompat.class);
            LOG.info("Iron's Spells 'n Spellbooks found: spell bonuses enabled");
        }
        if (Compat.APOTHEOSIS) {
            forge.register(dev.skillascension.compat.apotheosis.GemEvents.class);
            LOG.info("Apotheosis found: gem bonuses enabled");
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            forge.register(PstCooldownHud.class);
            forge.register(PstPointBadge.class);
        }
    }
}
