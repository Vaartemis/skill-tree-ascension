package dev.skillascension;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class AscensionConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.IntValue SINGLEPLAYER_LIMIT_BONUS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SINGLEPLAYER_LIMIT_TAGS;
    public static final ForgeConfigSpec.ConfigValue<String> HIDDEN_LIMIT_PATTERN;
    public static final ForgeConfigSpec.IntValue EFFECT_STACK_COOLDOWN;
    public static final ForgeConfigSpec.BooleanValue LEVEL_UP_MESSAGES;

    public static final ForgeConfigSpec.BooleanValue UI_ENABLED;
    public static final ForgeConfigSpec.BooleanValue HUB_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TREE_ORDER;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> HIDDEN_FROM_RAIL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TREE_COLORS;

    static {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("limits");
        SINGLEPLAYER_LIMIT_BONUS = common
                .comment("Extra allowed skills for the limitation tags below while playing alone (not on a server and not opened to LAN).",
                        "Example: trees limit the tag \"Class\" to 2 and you want solo players to pick 3 classes: set this to 1.")
                .defineInRange("singleplayerLimitBonus", 0, 0, 100);
        SINGLEPLAYER_LIMIT_TAGS = common
                .comment("Skill tree limitation tags that get the singleplayer bonus.")
                .defineListAllowEmpty("singleplayerLimitTags", List.of("Class"), o -> o instanceof String);
        HIDDEN_LIMIT_PATTERN = common
                .comment("Regex. Limitation tags matching it are enforced but not listed in skill tooltips (empty = show all).")
                .define("hiddenLimitPattern", "");
        common.pop();
        common.push("fixes");
        EFFECT_STACK_COOLDOWN = common
                .comment("Ticks before a PST effect bonus can add another stack to the same target (fixes effects stacking to max in one hit).")
                .defineInRange("effectStackCooldownTicks", 20, 0, 1200);
        common.pop();
        LEVEL_UP_MESSAGES = common
                .comment("Show a message above the hotbar when a repeatable skill levels up.")
                .define("levelUpMessages", true);
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("ui");
        UI_ENABLED = client.comment("Use the Skill Tree Ascension layout on the skill tree screen. Off = PST's own layout (points, levels and bonuses still work).")
                .define("enabled", true);
        HUB_ENABLED = client.comment("Replace PST's tree selection screen with the radial hub.")
                .define("hub", true);
        TREE_ORDER = client.comment("Order of skill trees in the side rail, hub and selection screen (tree ids or just their names). Trees not listed come after, by id.")
                .defineListAllowEmpty("treeOrder", List.of(), o -> o instanceof String);
        HIDDEN_FROM_RAIL = client.comment("Trees left out of the side rail and hub (tree ids or names). Trees opened by a point type button are left out automatically.")
                .defineListAllowEmpty("hiddenFromRail", List.of(), o -> o instanceof String);
        TREE_COLORS = client.comment("Colors of trees in the hub, as \"tree=#RRGGBB\" (tree id or name). Trees not listed get a color from a built-in palette.")
                .defineListAllowEmpty("treeColors", List.of(), o -> o instanceof String);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private AscensionConfig() {
    }

    /** Index of a tree in {@link #TREE_ORDER}, or the list size. */
    public static int order(net.minecraft.resources.ResourceLocation tree) {
        List<? extends String> order = TREE_ORDER.get();
        for (int i = 0; i < order.size(); i++) {
            String s = order.get(i);
            if (s.equals(tree.toString()) || s.equals(tree.getPath())) return i;
        }
        return order.size();
    }

    public static boolean hiddenFromRail(net.minecraft.resources.ResourceLocation tree) {
        if (dev.skillascension.common.LockedTrees.isLocked(tree)) return true;
        for (String s : HIDDEN_FROM_RAIL.get()) if (s.equals(tree.toString()) || s.equals(tree.getPath())) return true;
        return false;
    }
}
