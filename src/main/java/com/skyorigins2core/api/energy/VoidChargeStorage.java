package com.skyorigins2core.api.energy;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

// Public API: a ready-made Void Charge buffer for machines, like Forge's EnergyStorage.
// maxReceive / maxExtract limit what *other* blocks can push in or pull out per call;
// the owning machine adds and spends charge directly with generate() and consume().
// Override onChanged() to mark the block entity dirty.
public class VoidChargeStorage implements IVoidChargeStorage, INBTSerializable<Tag>
{
    protected int charge;
    protected int capacity;
    protected int maxReceive;
    protected int maxExtract;

    public VoidChargeStorage(int capacity, int maxReceive, int maxExtract)
    {
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    @Override
    public int receiveCharge(int maxReceive, boolean simulate)
    {
        if (!canReceive())
            return 0;
        int received = Math.min(capacity - charge, Math.min(this.maxReceive, maxReceive));
        if (!simulate && received > 0)
        {
            charge += received;
            onChanged();
        }
        return received;
    }

    @Override
    public int extractCharge(int maxExtract, boolean simulate)
    {
        if (!canExtract())
            return 0;
        int extracted = Math.min(charge, Math.min(this.maxExtract, maxExtract));
        if (!simulate && extracted > 0)
        {
            charge -= extracted;
            onChanged();
        }
        return extracted;
    }

    // For the owning machine: add charge regardless of maxReceive. Returns how much fitted.
    public int generate(int amount)
    {
        int added = Math.max(0, Math.min(capacity - charge, amount));
        if (added > 0)
        {
            charge += added;
            onChanged();
        }
        return added;
    }

    // For the owning machine: spend charge regardless of maxExtract. Returns whether there was enough.
    public boolean consume(int amount)
    {
        if (charge < amount)
            return false;
        charge -= amount;
        onChanged();
        return true;
    }

    @Override public int getChargeStored() { return charge; }
    @Override public int getMaxChargeStored() { return capacity; }
    @Override public boolean canReceive() { return maxReceive > 0; }
    @Override public boolean canExtract() { return maxExtract > 0; }

    protected void onChanged() {}

    @Override
    public Tag serializeNBT()
    {
        return IntTag.valueOf(charge);
    }

    @Override
    public void deserializeNBT(Tag tag)
    {
        if (tag instanceof IntTag intTag)
            charge = Math.max(0, Math.min(capacity, intTag.getAsInt()));
    }
}
