package dev.skillascension.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.skill.PassiveSkill;
import dev.skillascension.compat.OriginsCompat;
import dev.skillascension.pst.RepeatableSkills;
import dev.skillascension.stacks.EternalStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * One thing a player must have or have done before learning a skill. Written in a node rule as
 * {@code "requires": [ {...}, ... ]}. Every requirement looks at what the player HAS or HAS DONE (inventory,
 * vanilla statistics, advancements, stacks, skills), never at how an item was made, so items crafted by hand,
 * by machines or autocrafting systems, looted or traded all count the same.
 * <ul>
 * <li>{@code {"type":"item","item":"minecraft:shield"}} or {@code "tag":"forge:ingots/iron","count":8}: carry it.</li>
 * <li>{@code "type":"offer"}: same, and the items are taken when the skill is learned.</li>
 * <li>{@code {"type":"kills","entity":"minecraft:zombie"|"tag":"..."|"entities":[...],"count":10}}: vanilla kill statistics, summed.</li>
 * <li>{@code {"type":"used","item"|"tag":...,"count":N}}: vanilla "used" statistic (shots, drinks, meals...).</li>
 * <li>{@code {"type":"mined","block"|"tag":...,"count":N}}: vanilla "mined" statistic.</li>
 * <li>{@code {"type":"stat","stat":"minecraft:fish_caught","count":N}}: vanilla custom statistic.</li>
 * <li>{@code {"type":"stack","stack":"trophies","count":N}}: eternal stack value.</li>
 * <li>{@code {"type":"skill","skill":"ns:id","level":3}}: another skill learned (and at least that level if repeatable).</li>
 * <li>{@code {"type":"advancement","advancement":"ns:id"}}, {@code {"type":"origin",...}}, {@code {"type":"power",...}}.</li>
 * </ul>
 * Any requirement may carry {@code "text"} (a translation key or literal) to replace the generated description.
 */
public final class Requirement {
    public final String type;
    final ResourceLocation id;
    final List<ResourceLocation> ids;
    final ResourceLocation tag;
    final ResourceLocation layer;
    public final int count;
    final boolean active;
    final String text;

    Requirement(JsonObject json) {
        this.type = GsonHelper.getAsString(json, "type");
        String key = switch (type) {
            case "item", "offer", "used" -> "item";
            case "kills" -> "entity";
            case "mined" -> "block";
            case "stat" -> "stat";
            case "stack" -> "stack";
            case "skill" -> "skill";
            case "advancement" -> "advancement";
            case "origin" -> "origin";
            case "power" -> "power";
            default -> throw new IllegalArgumentException("unknown requirement type " + type);
        };
        this.id = json.has(key) && !"stack".equals(type) ? new ResourceLocation(GsonHelper.getAsString(json, key)) : null;
        List<ResourceLocation> list = new ArrayList<>();
        if (json.has("entities")) for (JsonElement e : GsonHelper.getAsJsonArray(json, "entities")) list.add(new ResourceLocation(e.getAsString()));
        if (json.has("items")) for (JsonElement e : GsonHelper.getAsJsonArray(json, "items")) list.add(new ResourceLocation(e.getAsString()));
        this.ids = List.copyOf(list);
        this.tag = json.has("tag") ? new ResourceLocation(GsonHelper.getAsString(json, "tag")) : null;
        this.layer = json.has("layer") ? new ResourceLocation(GsonHelper.getAsString(json, "layer")) : null;
        int def = "skill".equals(type) ? 1 : 1;
        this.count = Math.max(1, GsonHelper.getAsInt(json, "skill".equals(type) ? "level" : "count", def));
        this.active = GsonHelper.getAsBoolean(json, "active", false);
        this.text = "stack".equals(type) ? GsonHelper.getAsString(json, "stack") + "\u0000" + GsonHelper.getAsString(json, "text", "")
                : GsonHelper.getAsString(json, "text", null);
        if (id == null && tag == null && ids.isEmpty() && !"stack".equals(type)) throw new IllegalArgumentException(type + " requirement names nothing");
    }

    static List<Requirement> list(JsonObject json) {
        if (!json.has("requires")) return null;
        List<Requirement> out = new ArrayList<>();
        JsonElement r = json.get("requires");
        JsonArray array = r.isJsonArray() ? r.getAsJsonArray() : new JsonArray();
        if (!r.isJsonArray()) array.add(r);
        for (JsonElement e : array) out.add(new Requirement(e.getAsJsonObject()));
        return List.copyOf(out);
    }

    private String stackKey() {
        int i = text.indexOf('\u0000');
        return text.substring(0, i);
    }

    private String customText() {
        if (text == null) return null;
        if ("stack".equals(type)) {
            String t = text.substring(text.indexOf('\u0000') + 1);
            return t.isEmpty() ? null : t;
        }
        return text;
    }

    // ---------------------------------------------------------------- server checks

    /** How far the player is: a value compared against {@link #count}. */
    public long progress(ServerPlayer player) {
        return switch (type) {
            case "item", "offer" -> countItems(player.getInventory());
            case "kills" -> sumStat(player, Stats.ENTITY_KILLED, BuiltInRegistries.ENTITY_TYPE, Registries.ENTITY_TYPE);
            case "used" -> sumStat(player, Stats.ITEM_USED, BuiltInRegistries.ITEM, Registries.ITEM);
            case "mined" -> sumStat(player, Stats.BLOCK_MINED, BuiltInRegistries.BLOCK, Registries.BLOCK);
            case "stat" -> {
                ResourceLocation custom = BuiltInRegistries.CUSTOM_STAT.get(id);
                yield custom == null ? 0 : player.getStats().getValue(Stats.CUSTOM.get(custom));
            }
            case "stack" -> (long) Math.floor(EternalStacks.get(player, stackKey()));
            case "skill" -> skillLevel(player);
            case "advancement" -> {
                Advancement adv = player.server.getAdvancements().getAdvancement(id);
                yield adv != null && player.getAdvancements().getOrStartProgress(adv).isDone() ? 1 : 0;
            }
            case "origin" -> OriginsCompat.hasOrigin(player, id, layer) ? 1 : 0;
            case "power" -> OriginsCompat.hasPower(player, id, active) ? 1 : 0;
            default -> 0;
        };
    }

    public boolean test(ServerPlayer player) {
        return progress(player) >= count;
    }

    private boolean matchesItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (tag != null && stack.is(TagKey.create(Registries.ITEM, tag))) return true;
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.equals(id) || ids.contains(key);
    }

    private long countItems(Inventory inv) {
        long n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (matchesItem(s)) n += s.getCount();
        }
        return n;
    }

    /** Takes the offered items. Call only after {@link #test} passed. */
    public void consume(ServerPlayer player) {
        if (!"offer".equals(type)) return;
        Inventory inv = player.getInventory();
        int left = count;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!matchesItem(s)) continue;
            int take = Math.min(left, s.getCount());
            s.shrink(take);
            left -= take;
        }
        inv.setChanged();
    }

    private <T> long sumStat(ServerPlayer player, StatType<T> statType, net.minecraft.core.Registry<T> registry,
                             net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> key) {
        long n = 0;
        List<T> targets = new ArrayList<>();
        if (id != null && registry.containsKey(id)) targets.add(registry.get(id));
        for (ResourceLocation r : ids) if (registry.containsKey(r)) targets.add(registry.get(r));
        if (tag != null) {
            @SuppressWarnings("unchecked")
            TagKey<T> t = TagKey.create((net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>>) key, tag);
            registry.getTagOrEmpty(t).forEach(h -> targets.add(h.value()));
        }
        for (T target : targets.stream().distinct().toList()) {
            Stat<T> stat = statType.get(target);
            n += player.getStats().getValue(stat);
        }
        return n;
    }

    private long skillLevel(ServerPlayer player) {
        if (!PlayerSkillsProvider.hasSkills(player)) return 0;
        for (PassiveSkill s : PlayerSkillsProvider.get(player).getPlayerSkills()) {
            if (s != null && s.getId().equals(id)) {
                return RepeatableSkills.isRepeatable(s) ? 1 + RepeatableSkills.ranks(player).getOrDefault(id, 0) : 1;
            }
        }
        return 0;
    }

    // ---------------------------------------------------------------- text (client and server)

    public MutableComponent describe() {
        String custom = customText();
        if (custom != null) return Component.translatable(custom);
        String key = "skillascension.requirement." + type + (count > 1 && !"skill".equals(type) ? ".count" : "");
        return switch (type) {
            case "item", "offer", "used" -> Component.translatable(key, itemName(), count);
            case "kills" -> Component.translatable(key, entityName(), count);
            case "mined" -> Component.translatable(key, blockName(), count);
            case "stat" -> Component.translatable(key, Component.translatable("stat." + id.toLanguageKey()), count);
            case "stack" -> Component.translatable(key, Component.translatable("skillascension.stack." + stackKey().replace(':', '.')), count);
            case "skill" -> count > 1
                    ? Component.translatable("skillascension.requirement.skill.level", skillName(), count)
                    : Component.translatable(key, skillName());
            case "advancement" -> Component.translatable(key, Component.translatable("advancements." + id.getPath().replace('/', '.') + ".title"));
            case "origin" -> Component.translatable(key, Component.translatable("origin." + id.getNamespace() + "." + id.getPath() + ".name"));
            case "power" -> Component.translatable(key, Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".name"));
            default -> Component.literal(type);
        };
    }

    private Component skillName() {
        return Component.translatable("skill." + id.getNamespace() + "." + id.getPath() + ".name");
    }

    private Component itemName() {
        if (tag != null) return Component.translatable("skillascension.tag." + tag.toLanguageKey());
        ResourceLocation first = id != null ? id : ids.get(0);
        Item item = BuiltInRegistries.ITEM.get(first);
        MutableComponent name = item.getDescription().copy();
        if (id == null && ids.size() > 1) name = Component.translatable("skillascension.requirement.any_of", name, ids.size() - 1);
        return name;
    }

    private Component blockName() {
        if (tag != null) return Component.translatable("skillascension.tag." + tag.toLanguageKey());
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block.getName();
    }

    private Component entityName() {
        if (tag != null) return Component.translatable("skillascension.tag." + tag.toLanguageKey());
        if (id != null) return entity(id);
        if (ids.size() == 1) return entity(ids.get(0));
        MutableComponent out = Component.empty();
        int shown = Math.min(3, ids.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) out.append(Component.translatable("skillascension.requirement.or"));
            out.append(entity(ids.get(i)));
        }
        if (ids.size() > shown) out.append(Component.translatable("skillascension.requirement.more", ids.size() - shown));
        return out;
    }

    private static Component entity(ResourceLocation rl) {
        EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(rl);
        return t.getDescription();
    }

    public static ChatFormatting color(boolean met) {
        return met ? ChatFormatting.GREEN : ChatFormatting.RED;
    }
}
