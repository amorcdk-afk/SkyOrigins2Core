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

    // A view that only lets other blocks push charge in, e.g. a battery's input faces
    public static IVoidChargeStorage receiveOnly(IVoidChargeStorage storage)
    {
        return new Restricted(storage, true, false);
    }

    // A view that only lets other blocks pull charge out, e.g. a generator or a battery's output face
    public static IVoidChargeStorage extractOnly(IVoidChargeStorage storage)
    {
        return new Restricted(storage, false, true);
    }

    private record Restricted(IVoidChargeStorage storage, boolean receive, boolean extract) implements IVoidChargeStorage
    {
        @Override public int receiveCharge(int maxReceive, boolean simulate) { return receive ? storage.receiveCharge(maxReceive, simulate) : 0; }
        @Override public int extractCharge(int maxExtract, boolean simulate) { return extract ? storage.extractCharge(maxExtract, simulate) : 0; }
        @Override public int getChargeStored() { return storage.getChargeStored(); }
        @Override public int getMaxChargeStored() { return storage.getMaxChargeStored(); }
        @Override public boolean canReceive() { return receive && storage.canReceive(); }
        @Override public boolean canExtract() { return extract && storage.canExtract(); }
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
