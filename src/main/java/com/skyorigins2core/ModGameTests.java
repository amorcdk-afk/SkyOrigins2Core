package com.skyorigins2core;

import com.skyorigins2core.condenser.VoidCondenserBlockEntity;
import com.skyorigins2core.island.IslandRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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

    // A placed core grows its island, turns into the island's surface block and registers the island
    @GameTest(template = "empty_3x3", timeoutTicks = 300)
    public static void islandCoreGrowsAndRegisters(GameTestHelper helper)
    {
        BlockPos core = MIDDLE.above();
        helper.setBlock(core, SkyOrigins2Core.VOID_ISLAND_CORE.get());
        BlockPos absolute = helper.absolutePos(core);
        Set<BlockState> surfaces = Set.of(Blocks.GRASS_BLOCK.defaultBlockState(), Blocks.SAND.defaultBlockState(),
                Blocks.CRIMSON_NYLIUM.defaultBlockState());

        helper.succeedWhen(() -> {
            helper.assertTrue(surfaces.contains(helper.getBlockState(core)), "Core hasn't finished growing yet");
            boolean registered = IslandRegistry.get(helper.getLevel()).islands().stream()
                    .anyMatch(island -> island.center().equals(absolute));
            helper.assertTrue(registered, "Grown island wasn't registered");
        });
    }
}
