package com.skyorigins2core.api.energy;

import com.skyorigins2core.api.SkyOriginsCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

// Public API: Void Charge constants and helpers.
public final class VoidCharge
{
    // Forge Energy per Void Charge, used by the Charge Converter and anything that accepts both
    public static final int FE_PER_VC = 4;

    private VoidCharge() {}

    // The Void Charge storage on `side` of the block at `pos`, if it has one
    public static Optional<IVoidChargeStorage> at(Level level, BlockPos pos, Direction side)
    {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity == null ? Optional.empty()
                : blockEntity.getCapability(SkyOriginsCapabilities.VOID_CHARGE, side).resolve();
    }

    // Moves up to `amount` VC from `from` into whatever accepts charge on `side` of `pos`. Returns how much moved.
    public static int pushTo(IVoidChargeStorage from, Level level, BlockPos pos, Direction side, int amount)
    {
        return at(level, pos.relative(side), side.getOpposite()).map(target -> {
            int offered = from.extractCharge(amount, true);
            int accepted = target.receiveCharge(offered, false);
            from.extractCharge(accepted, false);
            return accepted;
        }).orElse(0);
    }
}
