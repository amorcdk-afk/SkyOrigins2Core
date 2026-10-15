package com.skyorigins2core.mana;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.function.BiConsumer;

// /skyorigins mana get <players>
// /skyorigins mana set <players> <amount>
// /skyorigins mana add <players> <amount>   (negative to remove)
public final class ManaCommands
{
    private ManaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("skyorigins")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mana")
                        .then(Commands.literal("get")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .executes(context -> get(context.getSource(), EntityArgument.getPlayers(context, "players")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                .executes(context -> change(context.getSource(), EntityArgument.getPlayers(context, "players"),
                                                        IntegerArgumentType.getInteger(context, "amount"), PlayerMana::set)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("players", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                .executes(context -> change(context.getSource(), EntityArgument.getPlayers(context, "players"),
                                                        IntegerArgumentType.getInteger(context, "amount"), PlayerMana::add)))))));
    }

    private static int get(CommandSourceStack source, Collection<ServerPlayer> players)
    {
        for (ServerPlayer player : players)
            ManaEvents.of(player).ifPresent(mana -> source.sendSuccess(() -> Component.translatable(
                    "commands.skyorigins2core.mana.get", player.getDisplayName(), mana.getMana(), mana.getMaxMana()), false));
        return players.size();
    }

    private static int change(CommandSourceStack source, Collection<ServerPlayer> players, int amount,
                              BiConsumer<PlayerMana, Integer> action)
    {
        for (ServerPlayer player : players)
            ManaEvents.of(player).ifPresent(mana -> {
                action.accept(mana, amount);
                source.sendSuccess(() -> Component.translatable(
                        "commands.skyorigins2core.mana.get", player.getDisplayName(), mana.getMana(), mana.getMaxMana()), true);
            });
        return players.size();
    }
}
