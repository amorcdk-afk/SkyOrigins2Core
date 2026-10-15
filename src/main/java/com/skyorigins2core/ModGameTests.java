package com.skyorigins2core;

import com.skyorigins2core.condenser.VoidCondenserBlockEntity;
import com.skyorigins2core.island.IslandDefinition;
import com.skyorigins2core.island.IslandDefinitions;
import com.skyorigins2core.island.IslandRegistry;
import com.skyorigins2core.island.IslandShape;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Run with: gradlew runGameTestServer (also available in game via /test runall)
@GameTestHolder(SkyOrigins2Core.MODID)
@PrefixGameTestTemplate(false)
public class ModGameTests
{
    private static final BlockPos MIDDLE = new BlockPos(1, 1, 1);

    // Given power and mana, a condenser finishes a cycle and puts something in its buffer
    @GameTest(template = "empty_3x3", timeoutTicks = 400)
    public static void voidCondenserProducesOutput(GameTestHelper helper)
    {
        helper.setBlock(MIDDLE, SkyOrigins2Core.VOID_CONDENSER.get());
        if (!(helper.getBlockEntity(MIDDLE) instanceof VoidCondenserBlockEntity condenser))
        {
            helper.fail("No Void Condenser block entity", MIDDLE);
            return;
        }
        // The energy input is capped per call, so fill it in several pushes, like a cable would
        condenser.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
            for (int i = 0; i < 20; i++)
                energy.receiveEnergy(VoidCondenserBlockEntity.MAX_ENERGY_INPUT, false);
        });
        condenser.receiveMana(5_000);

        helper.succeedWhen(() -> {
            boolean hasOutput = false;
            for (int i = 0; i < condenser.output().getSlots(); i++)
                hasOutput |= !condenser.output().getStackInSlot(i).isEmpty();
            helper.assertTrue(hasOutput, "Condenser buffer is still empty");
        });
    }

    // A placed core grows its island, turns into the island's surface block (or water, for a reef) and registers it
    @GameTest(template = "empty_3x3", timeoutTicks = 300)
    public static void islandCoreGrowsAndRegisters(GameTestHelper helper)
    {
        BlockPos core = MIDDLE.above();
        helper.setBlock(core, SkyOrigins2Core.VOID_ISLAND_CORE.get());
        BlockPos absolute = helper.absolutePos(core);
        Set<BlockState> surfaces = Set.of(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.SAND.defaultBlockState(),
                Blocks.CRIMSON_NYLIUM.defaultBlockState(), Blocks.WATER.defaultBlockState());

        helper.succeedWhen(() -> {
            helper.assertTrue(surfaces.contains(helper.getBlockState(core)), "Core hasn't finished growing yet");
            boolean registered = IslandRegistry.get(helper.getLevel()).islands().stream()
                    .anyMatch(island -> island.center().equals(absolute));
            helper.assertTrue(registered, "Grown island wasn't registered");
        });
    }

    // A reef island placed in its planned order keeps its water inside the pool: none spills over the rim
    // or falls into the void, and the pool is still full of source blocks
    @GameTest(template = "empty_3x3", timeoutTicks = 200)
    public static void reefPoolStaysInside(GameTestHelper helper)
    {
        IslandDefinition reef = IslandDefinitions.get(ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "reef_island"));
        if (reef == null)
        {
            helper.fail("No reef_island definition loaded");
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(MIDDLE).above(20); // well clear of the ground and other tests
        List<IslandShape.Placement> plan = IslandShape.plan(reef, center);
        for (IslandShape.Placement placement : plan)
            level.setBlock(placement.pos(), placement.state(), Block.UPDATE_ALL);
        level.setBlock(center, reef.coreBlock().defaultBlockState(), Block.UPDATE_ALL);

        Set<BlockPos> pool = new HashSet<>();
        pool.add(center);
        for (IslandShape.Placement placement : plan)
            if (placement.state().is(Blocks.WATER))
                pool.add(placement.pos());

        // Give any escaping water time to flow, then look around and below the whole island
        helper.runAfterDelay(60, () -> {
            int r = reef.radius() + 2;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -reef.depth() - 8, -r), center.offset(r, 1, r)))
            {
                boolean water = level.getFluidState(pos).is(FluidTags.WATER);
                if (water && !pool.contains(pos))
                    helper.fail("Water escaped the pool at " + pos.toShortString());
                if (pool.contains(pos) && !level.getFluidState(pos).isSource())
                    helper.fail("Pool block at " + pos.toShortString() + " isn't a water source");
            }
            helper.succeed();
        });
    }
}
