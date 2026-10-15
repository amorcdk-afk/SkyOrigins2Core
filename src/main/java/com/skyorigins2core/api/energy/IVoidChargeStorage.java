package com.skyorigins2core.api.energy;

// Public API: something that stores Void Charge (VC), SkyOrigins' own energy. Works like Forge's
// IEnergyStorage. Expose it from a block entity via the SkyOriginsCapabilities.VOID_CHARGE capability;
// VoidChargeStorage is a ready-made implementation. 1 VC = VoidCharge.FE_PER_VC Forge Energy.
public interface IVoidChargeStorage
{
    // Adds up to maxReceive VC; returns how much was (or, when simulating, would be) accepted
    int receiveCharge(int maxReceive, boolean simulate);

    // Removes up to maxExtract VC; returns how much was (or would be) removed
    int extractCharge(int maxExtract, boolean simulate);

    int getChargeStored();

    int getMaxChargeStored();

    boolean canReceive();

    boolean canExtract();
}
