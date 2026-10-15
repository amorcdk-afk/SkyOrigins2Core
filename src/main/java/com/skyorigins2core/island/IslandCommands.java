package com.skyorigins2core.island;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

// /skyorigins islands            list the islands this dimension knows about
// /skyorigins islands add [r]    register the island you're standing on (e.g. one grown before islands were saved)
// /skyorigins islands clear      forget every island in this dimension (blocks and biome stay)
// /skyorigins islands debug      show each spawn attempt's result on your action bar
public final class IslandCommands
{
    private static final int DEFAULT_RADIUS = 6;

    private IslandCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("skyorigins")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("islands")
                        .executes(context -> list(context.getSource()))
                        .then(Commands.literal("add")
                                .executes(context -> add(context.getSource(), DEFAULT_RADIUS))
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 16))
                                        .executes(context -> add(context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("clear")
                                .executes(context -> clear(context.getSource())))
                        .then(Commands.literal("debug")
                                .executes(context -> toggleDebug(context.getSource())))));
    }

    private static int clear(CommandSourceStack source)
    {
        int removed = IslandRegistry.get(source.getLevel()).clear();
        source.sendSuccess(() -> Component.literal("Forgot " + removed + " island(s) in this dimension (their blocks and biome stay)"), true);
        return removed;
    }

    private static int list(CommandSourceStack source)
    {
        ServerLevel level = source.getLevel();
        List<IslandRegistry.Island> islands = IslandRegistry.get(level).islands();
        if (islands.isEmpty())
        {
            source.sendSuccess(() -> Component.literal("No islands registered in this dimension. Grow one with a core, or stand on one and use /skyorigins islands add"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal(islands.size() + " island(s) in this dimension:"), false);
        for (IslandRegistry.Island island : islands)
        {
            BlockPos c = island.center();
            int distance = (int) Math.sqrt(c.distToCenterSqr(source.getPosition()));
            source.sendSuccess(() -> Component.literal("  " + c.getX() + " " + c.getY() + " " + c.getZ()
                    + "  radius " + island.radius() + ", " + distance + " blocks away"), false);
        }
        return islands.size();
    }

    private static int add(CommandSourceStack source, int radius)
    {
        BlockPos center = BlockPos.containing(source.getPosition()).below();
        IslandRegistry.get(source.getLevel()).add(center, radius);
        source.sendSuccess(() -> Component.literal("Registered island at " + center.getX() + " " + center.getY() + " " + center.getZ()
                + " with radius " + radius), true);
        return 1;
    }

    private static int toggleDebug(CommandSourceStack source)
    {
        IslandSpawner.debug = !IslandSpawner.debug;
        source.sendSuccess(() -> Component.literal("Island spawn debug " + (IslandSpawner.debug ? "on" : "off")), true);
        return 1;
    }
}
