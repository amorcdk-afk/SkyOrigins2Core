package com.skyorigins2core.api;

import com.skyorigins2core.api.energy.IVoidChargeStorage;
import com.skyorigins2core.api.mana.IPlayerMana;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

// Public API: the capabilities other mods use to reach SkyOrigins systems.
//   PLAYER_MANA  - on players: their personal mana pool (see ManaApi for helpers)
//   VOID_CHARGE  - on block entities: Void Charge storage, the mod's own energy (see VoidCharge)
public final class SkyOriginsCapabilities
{
    public static final Capability<IPlayerMana> PLAYER_MANA = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IVoidChargeStorage> VOID_CHARGE = CapabilityManager.get(new CapabilityToken<>() {});

    private SkyOriginsCapabilities() {}
}
