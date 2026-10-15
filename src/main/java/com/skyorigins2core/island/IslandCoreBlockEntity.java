package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class IslandCoreBlockEntity extends BlockEntity
{
    // How far through the island's placement plan this core has got; saved, so growth resumes after a reload
    private int progress;
    private List<IslandShape.Placement> plan;

    public IslandCoreBlockEntity(BlockPos pos, BlockState state)
    {
        super(SkyOrigins2Core.ISLAND_CORE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, IslandCoreBlockEntity core)
    {
        if (level instanceof ServerLevel serverLevel && state.getBlock() instanceof IslandCoreBlock block)
            core.grow(serverLevel, block);
    }

    private void grow(ServerLevel level, IslandCoreBlock block)
    {
        IslandDefinition definition = IslandDefinitions.get(block.islandId());
        if (definition == null)
        {
            SkyOrigins2Core.LOGGER.error("Island core at {} has no island definition {}", worldPosition, block.islandId());
            level.setBlockAndUpdate(worldPosition, Blocks.DIRT.defaultBlockState());
            return;
        }
        if (plan == null)
            plan = IslandShape.plan(definition, worldPosition);

        int budget = definition.blocksPerTick();
        while (budget > 0 && progress < plan.size())
        {
            IslandShape.Placement placement = plan.get(progress);
            BlockPos pos = placement.pos();
            if (level.isOutsideBuildHeight(pos))
            {
                progress++; // below the world's floor or above its ceiling: skip, it can never be placed
                continue;
            }
            if (!level.isLoaded(pos))
                break; // try again next tick rather than skipping blocks in unloaded chunks
            progress++;

            // Never overwrite something already there, so cores can't erase builds
            BlockState existing = level.getBlockState(pos);
            if (existing.isAir() || existing.canBeReplaced())
            {
                level.setBlock(pos, placement.state(), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.2, 0.1, 0.2, 0.0);
                budget--;
            }
        }
        setChanged();

        if (progress >= plan.size())
            finish(level, definition);
    }

    private void finish(ServerLevel level, IslandDefinition definition)
    {
        BiomeStamper.stamp(level, worldPosition, definition);
        IslandRegistry.get(level).add(worldPosition, definition.radius());
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.8F);
        // Replacing the core removes this block entity
        level.setBlockAndUpdate(worldPosition, definition.surface().defaultBlockState());
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putInt("Progress", progress);
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        progress = tag.getInt("Progress");
    }
}
