package com.skyorigins2core;

import com.skyorigins2core.api.energy.IVoidChargeStorage;
import com.skyorigins2core.api.energy.VoidCharge;
import com.skyorigins2core.api.energy.VoidChargeStorage;
import com.skyorigins2core.api.mana.ManaApi;
import com.skyorigins2core.condenser.VoidCondenserBlockEntity;
import com.skyorigins2core.island.IslandDefinition;
import com.skyorigins2core.island.IslandDefinitions;
import com.skyorigins2core.island.IslandRegistry;
import com.skyorigins2core.island.IslandShape;
import com.skyorigins2core.island.IslandTemplates;
import com.skyorigins2core.mana.ManaEvents;
import com.skyorigins2core.mana.PlayerMana;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
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

    // A player's mana can't go below zero or above their maximum, can't be overspent, and regenerates
    @GameTest(template = "empty_3x3")
    public static void playerManaRespectsLimits(GameTestHelper helper)
    {
        Player player = helper.makeMockPlayer();
        PlayerMana mana = ManaEvents.of(player).orElse(null);
        if (mana == null)
        {
            helper.fail("Player has no mana capability");
            return;
        }
        int max = mana.getMaxMana();
        helper.assertTrue(max > 0, "Max mana should come from the max_mana attribute, got " + max);

        mana.set(0);
        helper.assertFalse(mana.consume(1, false), "Spent mana the player didn't have");
        mana.add(max + 50);
        helper.assertTrue(mana.getMana() == max, "Mana went over the maximum: " + mana.getMana());
        helper.assertTrue(mana.consume(30, true) && mana.getMana() == max, "Simulated spend changed mana");
        helper.assertTrue(mana.consume(30, false) && mana.getMana() == max - 30, "Spend didn't remove 30");
        helper.assertTrue(ManaApi.has(player, max - 30) && !ManaApi.has(player, max - 29), "ManaApi.has disagrees with the pool");

        double regen = mana.getRegenPerSecond();
        mana.regenerate(regen * 5);
        int expected = (int) Math.min(max, max - 30 + regen * 5);
        helper.assertTrue(mana.getMana() == expected, "Regen gave " + mana.getMana() + ", expected " + expected);
        helper.succeed();
    }

    // The Void Charge API storage respects capacity and per-call limits, and its views and save/load work
    @GameTest(template = "empty_3x3")
    public static void voidChargeStorageApi(GameTestHelper helper)
    {
        VoidChargeStorage storage = new VoidChargeStorage(1_000, 100, 50);
        helper.assertTrue(storage.receiveCharge(500, false) == 100, "receive should be capped at maxReceive");
        helper.assertTrue(storage.receiveCharge(100, true) == 100 && storage.getChargeStored() == 100, "simulated receive changed the storage");
        helper.assertTrue(storage.extractCharge(500, false) == 50, "extract should be capped at maxExtract");
        helper.assertTrue(storage.generate(5_000) == 950 && storage.getChargeStored() == 1_000, "generate should fill only to capacity");
        helper.assertTrue(storage.receiveCharge(10, false) == 0, "a full storage accepted charge");
        helper.assertTrue(storage.consume(400) && !storage.consume(10_000) && storage.getChargeStored() == 600, "consume is all-or-nothing");

        IVoidChargeStorage in = VoidCharge.receiveOnly(storage);
        IVoidChargeStorage out = VoidCharge.extractOnly(storage);
        helper.assertTrue(in.extractCharge(10, false) == 0 && !in.canExtract(), "receive-only view let charge out");
        helper.assertTrue(out.receiveCharge(10, false) == 0 && !out.canReceive(), "extract-only view let charge in");
        helper.assertTrue(in.receiveCharge(10, false) == 10 && out.extractCharge(10, false) == 10, "views don't reach the storage");

        VoidChargeStorage copy = new VoidChargeStorage(1_000, 100, 50);
        copy.deserializeNBT(storage.serializeNBT());
        helper.assertTrue(copy.getChargeStored() == storage.getChargeStored(), "save/load lost charge");
        helper.succeed();
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
        level.setBlock(center, reef.coreState(), Block.UPDATE_ALL);

        Set<BlockPos> pool = new HashSet<>();
        pool.add(center);
        for (IslandShape.Placement placement : plan)
            if (placement.state().getFluidState().is(FluidTags.WATER))
                pool.add(placement.pos()); // water, plus waterlogged coral and seagrass

        // Give any escaping water time to flow, then look around and below the whole island
        helper.runAfterDelay(60, () -> {
            int r = reef.radius() + 2;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -reef.depth() - 8, -r), center.offset(r, 1, r)))
            {
                boolean water = level.getFluidState(pos).is(FluidTags.WATER);
                if (water && !pool.contains(pos))
                    helper.fail("Water escaped the pool at " + pos.subtract(center).toShortString() + " from the centre (" + level.getBlockState(pos) + ")");
                if (pool.contains(pos) && !level.getFluidState(pos).isSource())
                    helper.fail("Pool block at " + pos.toShortString() + " isn't a water source");
            }
            helper.succeed();
        });
    }

    // Every island that names a template has that template loaded, with nothing that could fall
    @GameTest(template = "empty_3x3")
    public static void islandTemplatesLoad(GameTestHelper helper)
    {
        for (String name : List.of("void_island", "desert_island", "crust_island", "reef_island"))
        {
            IslandDefinition island = IslandDefinitions.get(ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, name));
            helper.assertTrue(island != null && island.template().isPresent(), name + " has no template");
            IslandTemplates.Template template = IslandTemplates.get(island.template().get());
            helper.assertTrue(template != null, name + "'s template " + island.template().get() + " isn't loaded");
            Set<BlockPos> filled = new HashSet<>();
            for (IslandShape.Placement block : template.blocks())
                filled.add(block.pos());
            filled.add(BlockPos.ZERO);
            for (IslandShape.Placement block : template.blocks())
                if (block.state().getBlock() instanceof FallingBlock && !filled.contains(block.pos().below()))
                    helper.fail(name + " has " + block.state().getBlock() + " with nothing under it at " + block.pos().toShortString());
        }
        helper.succeed();
    }
}
