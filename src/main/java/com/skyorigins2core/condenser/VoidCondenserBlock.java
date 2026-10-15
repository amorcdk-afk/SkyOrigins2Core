package com.skyorigins2core.condenser;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// Right-click: show energy, mana, progress and buffer. Sneak-right-click with an empty hand: take the buffer.
public class VoidCondenserBlock extends BaseEntityBlock
{
    public VoidCondenserBlock(Properties properties)
    {
        super(properties);
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
        return new VoidCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return level.isClientSide ? null
                : createTickerHelper(type, SkyOrigins2Core.VOID_CONDENSER_ENTITY.get(), VoidCondenserBlockEntity::serverTick);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof VoidCondenserBlockEntity condenser))
            return InteractionResult.PASS;

        ItemStackHandler output = condenser.output();
        if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty())
        {
            for (int i = 0; i < output.getSlots(); i++)
            {
                ItemStack stack = output.extractItem(i, Integer.MAX_VALUE, false);
                if (!stack.isEmpty() && !player.getInventory().add(stack))
                    player.drop(stack, false);
            }
            return InteractionResult.CONSUME;
        }

        CondenserSettings.Settings settings = CondenserSettings.get();
        int items = 0;
        for (int i = 0; i < output.getSlots(); i++)
            items += output.getStackInSlot(i).getCount();
        int percent = condenser.progress() == 0 ? 0 : condenser.progress() * 100 / settings.ticks();
        player.displayClientMessage(Component.translatable("block.skyorigins2core.void_condenser.status",
                String.format("%,d", condenser.energyStored()), String.format("%,d", VoidCondenserBlockEntity.ENERGY_CAPACITY),
                String.format("%,d", condenser.mana()), String.format("%,d", VoidCondenserBlockEntity.MANA_CAPACITY),
                percent, items), true);
        return InteractionResult.CONSUME;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston)
    {
        // Drop the buffer when the block is broken
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof VoidCondenserBlockEntity condenser)
        {
            ItemStackHandler output = condenser.output();
            for (int i = 0; i < output.getSlots(); i++)
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), output.getStackInSlot(i));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
