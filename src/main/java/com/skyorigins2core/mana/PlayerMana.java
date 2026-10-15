package com.skyorigins2core.mana;

import com.skyorigins2core.api.mana.IPlayerMana;
import com.skyorigins2core.api.mana.ManaAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.INBTSerializable;

// A player's mana pool. Stored as a double so slow regen (e.g. 0.5/s) still accumulates.
public class PlayerMana implements IPlayerMana, INBTSerializable<CompoundTag>
{
    private final Player player;
    private double mana;
    // What the client was last told, so a sync is only sent when the visible numbers change
    private int sentMana = -1;
    private int sentMax = -1;

    public PlayerMana(Player player)
    {
        this.player = player;
    }

    @Override
    public int getMana()
    {
        return (int) Math.floor(mana);
    }

    @Override
    public int getMaxMana()
    {
        return (int) attribute(ManaAttributes.MAX_MANA.get());
    }

    public double getRegenPerSecond()
    {
        return attribute(ManaAttributes.MANA_REGEN.get());
    }

    private double attribute(Attribute attribute)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance == null ? 0.0 : instance.getValue();
    }

    @Override
    public boolean consume(int amount, boolean simulate)
    {
        if (amount <= 0)
            return true;
        if (mana < amount)
            return false;
        if (!simulate)
            mana -= amount;
        return true;
    }

    @Override
    public void add(int amount)
    {
        mana = Math.max(0, Math.min(getMaxMana(), mana + amount));
    }

    @Override
    public void set(int amount)
    {
        mana = Math.max(0, Math.min(getMaxMana(), amount));
    }

    public void regenerate(double amount)
    {
        if (mana < getMaxMana())
            mana = Math.min(getMaxMana(), mana + amount);
    }

    public void fill()
    {
        mana = getMaxMana();
    }

    // If the maximum dropped (an item with a +max_mana modifier was removed), don't keep the excess
    public void clampToMax()
    {
        if (mana > getMaxMana())
            mana = getMaxMana();
    }

    // True when the client needs an update; records the values as sent
    public boolean takeSyncChange()
    {
        int current = getMana();
        int max = getMaxMana();
        if (current == sentMana && max == sentMax)
            return false;
        sentMana = current;
        sentMax = max;
        return true;
    }

    // Makes the next tick send a sync, e.g. after login or a dimension change
    public void forceSync()
    {
        sentMana = -1;
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Mana", mana);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        mana = Math.max(0, tag.getDouble("Mana"));
    }

    // Copies mana across PlayerEvent.Clone (the old and new player are different objects)
    public void copyFrom(PlayerMana other)
    {
        mana = other.mana;
    }
}
