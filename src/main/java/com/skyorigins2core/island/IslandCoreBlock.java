package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// A placed core grows a random island from its pool (data/<ns>/island_pools/), then turns into the island's surface block
public class IslandCoreBlock extends BaseEntityBlock
{
    private final ResourceLocation poolId;

    public IslandCoreBlock(ResourceLocation poolId, Properties properties)
    {
        super(properties);
        this.poolId = poolId;
    }

    public ResourceLocation poolId()
    {
        return poolId;
    }

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new IslandCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, SkyOrigins2Core.ISLAND_CORE.get(), IslandCoreBlockEntity::serverTick);
    }
}
