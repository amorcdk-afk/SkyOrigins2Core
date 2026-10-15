package com.skyorigins2core.compat;

import com.skyorigins2core.condenser.VoidCondenserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaReceiver;

// Everything that touches Botania's classes lives here, and callers only reach it when Botania is
// installed, so the mod still loads without Botania.
public final class BotaniaCompat
{
    private BotaniaCompat() {}

    public static boolean isManaReceiver(Capability<?> capability)
    {
        return capability == BotaniaForgeCapabilities.MANA_RECEIVER;
    }

    // Lets mana spreaders fire bursts into the Void Condenser
    public static LazyOptional<ManaReceiver> manaReceiver(VoidCondenserBlockEntity condenser)
    {
        return LazyOptional.of(() -> new ManaReceiver()
        {
            @Override public Level getManaReceiverLevel() { return condenser.getLevel(); }
            @Override public BlockPos getManaReceiverPos() { return condenser.getBlockPos(); }
            @Override public int getCurrentMana() { return condenser.mana(); }
            @Override public boolean isFull() { return condenser.isManaFull(); }
            @Override public void receiveMana(int mana) { condenser.receiveMana(mana); }
            @Override public boolean canReceiveManaFromBursts() { return true; }
        });
    }
}
