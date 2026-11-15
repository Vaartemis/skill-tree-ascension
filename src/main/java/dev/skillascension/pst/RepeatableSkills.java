package dev.skillascension.pst;

import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.data.reloader.SkillsReloader;
import daripher.skilltree.skill.PassiveSkill;
import daripher.skilltree.skill.bonus.SkillBonus;
import daripher.skilltree.skill.bonus.player.AttributeBonus;
import dev.skillascension.AscensionConfig;
import dev.skillascension.data.NodeRule;
import dev.skillascension.data.NodeRules;
import dev.skillascension.net.AscensionNet;
import dev.skillascension.points.PointSources;
import dev.skillascension.points.PointWallet;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extra levels of repeatable skills. Level 1 is the normal PST learn; every level after it is a "rank" stored here.
 * Each rank adds the skill's bonuses once more (times bonus_per_level), plus free levels every N when the skill has no level cap.
 */
public final class RepeatableSkills {
    private static final String KEY = "skillascension_skill_ranks";
    private static final String MILESTONES_KEY = "skillascension_milestones";
    private static final String PERSISTED = "PlayerPersisted";
    private static final Map<UUID, Map<ResourceLocation, Integer>> CACHE = new ConcurrentHashMap<>();

    private RepeatableSkills() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CACHE.remove(event.getEntity().getUUID());
    }

    public static boolean isRepeatable(PassiveSkill skill) {
        return skill != null && NodeRules.of(skill).repeatable;
    }

    /** 0 = no limit. */
    public static int maxLevel(PassiveSkill skill) {
        return NodeRules.of(skill).maxLevel;
    }

    /** True when the skill cannot go past its level cap with this many extra ranks. */
    public static boolean atCap(PassiveSkill skill, int ranks) {
        int max = maxLevel(skill);
        return max > 0 && 1 + ranks >= max;
    }

    /** Free levels earned at this level (only for skills without a cap). */
    public static int freeLevels(PassiveSkill skill, int level) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        return rule.maxLevel > 0 || rule.freeLevelEvery <= 0 ? 0 : level / rule.freeLevelEvery;
    }

    /** How many extra copies of the skill's bonuses these ranks are worth. */
    public static double power(PassiveSkill skill, int rank) {
        if (rank <= 0) return 0;
        return (rank + freeLevels(skill, rank + 1)) * NodeRules.of(skill).bonusPerLevel;
    }

    public static boolean hasLearned(IPlayerSkills capability, PassiveSkill skill) {
        for (PassiveSkill learned : capability.getPlayerSkills()) {
            if (learned != null && learned.getId().equals(skill.getId())) return true;
        }
        return false;
    }

    public static Map<ResourceLocation, Integer> ranks(Player player) {
        if (!(player instanceof ServerPlayer)) return Collections.emptyMap();
        return CACHE.computeIfAbsent(player.getUUID(), id -> read(player));
    }

    public static int totalRanks(Player player) {
        int total = 0;
        for (int rank : ranks(player).values()) total += rank;
        return total;
    }

    public static void rankUp(ServerPlayer player, IPlayerSkills capability, PassiveSkill skill) {
        NodeRule.Resolved rule = NodeRules.of(skill);
        int current = ranks(player).getOrDefault(skill.getId(), 0);
        if (atCap(skill, current)) return;
        int cost = rule.levelCost;
        if (PointWallet.available(player, capability, skill) < cost) return;
        PointWallet.pay(player, capability, skill, cost);
        Map<ResourceLocation, Integer> ranks = new HashMap<>(ranks(player));
        int rank = current + 1;
        ranks.put(skill.getId(), rank);
        write(player, ranks);
        applyAttributes(player);
        AscensionNet.sendRanks(player);
        int level = rank + 1;
        if (AscensionConfig.LEVEL_UP_MESSAGES.get()) {
            player.displayClientMessage(Component.translatable("skillascension.skill_rank_up",
                    Component.translatable("skill." + skill.getId().getNamespace() + "." + skill.getId().getPath() + ".name"), level)
                    .withStyle(ChatFormatting.GOLD), true);
        }
        runMilestones(player, skill, rule, level);
    }

    private static void runMilestones(ServerPlayer player, PassiveSkill skill, NodeRule.Resolved rule, int level) {
        if (rule.milestones.isEmpty()) return;
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(PERSISTED);
        CompoundTag done = persisted.getCompound(MILESTONES_KEY);
        boolean changed = false;
        for (NodeRule.Milestone milestone : rule.milestones) {
            String key = skill.getId() + "#" + milestone.level;
            if (level < milestone.level || done.getBoolean(key)) continue;
            done.putBoolean(key, true);
            changed = true;
            CommandSourceStack source = player.createCommandSourceStack().withPermission(4).withSuppressedOutput();
            for (String command : milestone.commands) {
                player.server.getCommands().performPrefixedCommand(source, command);
            }
            if (milestone.message != null) player.sendSystemMessage(milestone.message.copy());
        }
        if (changed) {
            persisted.put(MILESTONES_KEY, done);
            data.put(PERSISTED, persisted);
        }
    }

    public static void onTreeReset(ServerPlayer player, IPlayerSkills capability) {
        Map<ResourceLocation, Integer> ranks = ranks(player);
        if (ranks.isEmpty()) return;
        ranks.forEach((id, rank) -> {
            PassiveSkill skill = SkillsReloader.getSkillById(id);
            if (skill == null) capability.grantSkillPoints(rank);
            else PointWallet.refund(player, capability, skill, rank * NodeRules.of(skill).levelCost);
        });
        write(player, new HashMap<>());
        applyAttributes(player);
        AscensionNet.sendRanks(player);
    }

    /** Drops ranks of skills that are gone, no longer repeatable or no longer learned, and refunds them. */
    public static void refresh(Player entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        CACHE.remove(player.getUUID());
        if (!PlayerSkillsProvider.hasSkills(player)) return;
        IPlayerSkills capability = PlayerSkillsProvider.get(player);
        Map<ResourceLocation, Integer> ranks = new HashMap<>(ranks(player));
        int refund = 0;
        boolean changed = false;
        for (Map.Entry<ResourceLocation, Integer> entry : Map.copyOf(ranks).entrySet()) {
            PassiveSkill skill = SkillsReloader.getSkillById(entry.getKey());
            if (skill == null || !isRepeatable(skill) || !hasLearned(capability, skill)) {
                if (skill == null) refund += entry.getValue();
                else PointWallet.refund(player, capability, skill, entry.getValue() * NodeRules.of(skill).levelCost);
                ranks.remove(entry.getKey());
                changed = true;
            }
        }
        if (changed) {
            capability.grantSkillPoints(refund);
            write(player, ranks);
            PstServerSkills.sync(player);
        }
        applyAttributes(player);
        AscensionNet.sendRanks(player);
        AscensionNet.sendPoints(player);
        PointSources.update(player);
    }

    /** Attribute bonuses are applied as permanent modifiers, one per skill and bonus index. */
    public static void applyAttributes(ServerPlayer player) {
        Map<ResourceLocation, Integer> ranks = ranks(player);
        applyMilestoneAttributes(player, ranks);
        for (PassiveSkill skill : SkillsReloader.getSkills().values()) {
            if (!isRepeatable(skill)) continue;
            double power = power(skill, ranks.getOrDefault(skill.getId(), 0));
            List<SkillBonus<?>> bonuses = skill.getBonuses();
            for (int i = 0; i < bonuses.size(); i++) {
                if (!(bonuses.get(i) instanceof AttributeBonus bonus) || bonus.getAttribute() == null) continue;
                AttributeInstance instance = player.getAttribute(bonus.getAttribute());
                if (instance == null) continue;
                UUID id = UUID.nameUUIDFromBytes(("skillascension:rank/" + skill.getId() + "/" + i).getBytes(StandardCharsets.UTF_8));
                if (bonus.isDynamic()) {
                    // ticked by PST while learned; drop it here once the level is gone so it does not linger
                    if (power <= 0) instance.removeModifier(id);
                    continue;
                }
                instance.removeModifier(id);
                if (power > 0) {
                    AttributeModifier base = bonus.getModifier();
                    instance.addPermanentModifier(new AttributeModifier(id, "Skill level", base.getAmount() * power, base.getOperation()));
                }
            }
        }
    }

    private static void applyMilestoneAttributes(ServerPlayer player, Map<ResourceLocation, Integer> ranks) {
        java.util.Set<ResourceLocation> learned = new java.util.HashSet<>();
        if (PlayerSkillsProvider.hasSkills(player)) {
            for (PassiveSkill s : PlayerSkillsProvider.get(player).getPlayerSkills()) if (s != null) learned.add(s.getId());
        }
        for (PassiveSkill skill : SkillsReloader.getSkills().values()) {
            if (!isRepeatable(skill)) continue;
            List<NodeRule.Milestone> milestones = NodeRules.of(skill).milestones;
            int level = learned.contains(skill.getId()) ? 1 + ranks.getOrDefault(skill.getId(), 0) : 0;
            for (int m = 0; m < milestones.size(); m++) {
                List<SkillBonus<?>> bonuses = milestones.get(m).bonuses();
                for (int i = 0; i < bonuses.size(); i++) {
                    if (!(bonuses.get(i) instanceof AttributeBonus bonus) || bonus.getAttribute() == null) continue;
                    AttributeInstance instance = player.getAttribute(bonus.getAttribute());
                    if (instance == null) continue;
                    UUID id = UUID.nameUUIDFromBytes(("skillascension:milestone/" + skill.getId() + "/" + m + "/" + i).getBytes(StandardCharsets.UTF_8));
                    if (bonus.isDynamic()) {
                        // ticked by PST through MilestoneBonuses; only clean up when the milestone is not reached
                        if (level < milestones.get(m).level) instance.removeModifier(id);
                        continue;
                    }
                    instance.removeModifier(id);
                    if (level >= milestones.get(m).level) {
                        AttributeModifier base = bonus.getModifier();
                        instance.addPermanentModifier(new AttributeModifier(id, "Skill milestone", base.getAmount(), base.getOperation()));
                    }
                }
            }
        }
    }

    private static Map<ResourceLocation, Integer> read(Player player) {
        CompoundTag tag = player.getPersistentData().getCompound(PERSISTED).getCompound(KEY);
        Map<ResourceLocation, Integer> ranks = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            int rank = tag.getInt(key);
            if (id != null && rank > 0) ranks.put(id, rank);
        }
        return ranks;
    }

    private static void write(Player player, Map<ResourceLocation, Integer> ranks) {
        CompoundTag tag = new CompoundTag();
        ranks.forEach((id, rank) -> tag.putInt(id.toString(), rank));
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(PERSISTED);
        persisted.put(KEY, tag);
        data.put(PERSISTED, persisted);
        CACHE.put(player.getUUID(), Collections.unmodifiableMap(new HashMap<>(ranks)));
    }
}
