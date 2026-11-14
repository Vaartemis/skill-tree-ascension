package dev.skillascension.points;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import dev.skillascension.data.PointType;
import dev.skillascension.data.PointTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;

/**
 * Adds point types to PST's own command instead of a second command:
 * /skilltree points add|set &lt;player&gt; &lt;amount&gt; [type] and /skilltree points get &lt;player&gt; [type].
 * Brigadier merges nodes with the same name, so PST's "add/set &lt;player&gt; &lt;amount&gt;" keep working
 * and only gain an optional type at the end.
 */
public final class PointsCommand {
    private PointsCommand() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("skilltree").requires(s -> s.hasPermission(2))
                .then(Commands.literal("points")
                        .then(Commands.literal("add").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .then(type().executes(c -> change(c, true))))))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .then(type().executes(c -> change(c, false))))))
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> get(c, null))
                                .then(type().executes(c -> get(c, typeArg(c))))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> type() {
        return Commands.argument("type", ResourceLocationArgument.id())
                .suggests((c, b) -> SharedSuggestionProvider.suggestResource(PointTypes.all().stream().map(t -> t.id), b));
    }

    private static PointType typeArg(CommandContext<CommandSourceStack> c) {
        ResourceLocation id = ResourceLocationArgument.getId(c, "type");
        if (!PointTypes.exists(id)) {
            c.getSource().sendFailure(Component.literal("Unknown point type " + id));
            return null;
        }
        return PointTypes.get(id);
    }

    private static int current(ServerPlayer player, PointType type) {
        return type.nativePoints ? PlayerSkillsProvider.get(player).getSkillPoints() : PointWallet.get(player, type.id);
    }

    private static int change(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
        PointType type = typeArg(c);
        if (type == null) return 0;
        int amount = IntegerArgumentType.getInteger(c, "amount");
        Collection<ServerPlayer> players = java.util.List.of(EntityArgument.getPlayer(c, "player"));
        for (ServerPlayer player : players) {
            int delta = add ? amount : Math.max(0, amount) - current(player, type);
            PointWallet.add(player, type.id, delta);
            if (add && amount > 0) {
                player.displayClientMessage(Component.translatable("skillascension.points.gained", amount, type.name).withStyle(ChatFormatting.GOLD), false);
            }
        }
        c.getSource().sendSuccess(() -> Component.literal(type.id + (add ? " +" : " = ") + amount + " (" + players.size() + " player(s))"), true);
        return players.size();
    }

    private static int get(CommandContext<CommandSourceStack> c, PointType only) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        int last = 0;
        for (PointType type : only != null ? java.util.List.of(only) : PointTypes.all()) {
            last = current(player, type);
            String line = player.getGameProfile().getName() + ": " + last + " " + type.name.getString() + " (" + type.id + ")";
            c.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return last;
    }
}
