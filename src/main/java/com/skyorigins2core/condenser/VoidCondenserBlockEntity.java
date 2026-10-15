package com.skyorigins2core.condenser;

import com.skyorigins2core.SkyOrigins2Core;
import com.skyorigins2core.compat.BotaniaCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// Turns Forge Energy + Botania mana into sieve inputs, one cycle at a time (see CondenserSettings)
public class VoidCondenserBlockEntity extends BlockEntity
{
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_ENERGY_INPUT = 1_000;
    public static final int MANA_CAPACITY = 50_000;
    public static final int OUTPUT_SLOTS = 9;
    private static final boolean BOTANIA = ModList.get().isLoaded("botania");

    // Receive-only: cables can push in, nothing can pull energy out; the condenser spends it with consume()
    private class CondenserEnergy extends EnergyStorage
    {
        CondenserEnergy()
        {
            super(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate)
        {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate)
                setChanged();
            return received;
        }

        void consume(int amount)
        {
            energy = Math.max(0, energy - amount);
        }
    }

    private final CondenserEnergy energy = new CondenserEnergy();
    private final ItemStackHandler output = new ItemStackHandler(OUTPUT_SLOTS)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
        }
    };
    // Extract-only view for hoppers and pipes: the buffer is for output, nothing can be inserted
    private final IItemHandler outputView = new IItemHandler()
    {
        @Override public int getSlots() { return output.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return output.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) { return output.extractItem(slot, amount, simulate); }
        @Override public int getSlotLimit(int slot) { return output.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
    };
    private LazyOptional<EnergyStorage> energyCap = LazyOptional.of(() -> energy);
    private LazyOptional<IItemHandler> itemCap = LazyOptional.of(() -> outputView);
    @Nullable
    private LazyOptional<?> manaCap;

    private int mana;
    // Ticks into the current cycle; 0 means idle (mana is paid when a cycle starts)
    private int progress;

    public VoidCondenserBlockEntity(BlockPos pos, BlockState state)
    {
        super(SkyOrigins2Core.VOID_CONDENSER_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VoidCondenserBlockEntity condenser)
    {
        if (level instanceof ServerLevel serverLevel)
            condenser.tick(serverLevel);
    }

    private void tick(ServerLevel level)
    {
        CondenserSettings.Settings settings = CondenserSettings.get();
        if (progress == 0)
        {
            // Start a cycle only if there's an empty slot for its output and the mana to pay for it
            if (!hasEmptySlot() || mana < settings.mana())
                return;
            mana -= settings.mana();
            progress = 1;
            setChanged();
        }
        if (energy.getEnergyStored() < settings.energyPerTick())
            return; // paused until power arrives
        energy.consume(settings.energyPerTick());
        progress++;
        if (progress > settings.ticks())
        {
            ItemStack result = settings.roll(level.random);
            if (!result.isEmpty())
                addOutput(result);
            progress = 0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1,
                    worldPosition.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
        }
        setChanged();
    }

    private boolean hasEmptySlot()
    {
        for (int i = 0; i < output.getSlots(); i++)
            if (output.getStackInSlot(i).isEmpty())
                return true;
        return false;
    }

    // Merges into existing stacks first; a cycle only starts with an empty slot, so a result always fits
    private void addOutput(ItemStack stack)
    {
        ItemStack remaining = stack;
        for (int i = 0; i < output.getSlots() && !remaining.isEmpty(); i++)
            remaining = output.insertItem(i, remaining, false);
    }

    public int energyStored() { return energy.getEnergyStored(); }
    public int mana() { return mana; }
    public int progress() { return progress; }
    public ItemStackHandler output() { return output; }

    public boolean isManaFull()
    {
        return mana >= MANA_CAPACITY;
    }

    public void receiveMana(int amount)
    {
        mana = Math.max(0, Math.min(MANA_CAPACITY, mana + amount));
        setChanged();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ENERGY)
            return energyCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER)
            return itemCap.cast();
        // Only touch Botania's classes when Botania is installed
        if (BOTANIA && BotaniaCompat.isManaReceiver(cap))
        {
            if (manaCap == null)
                manaCap = BotaniaCompat.manaReceiver(this);
            return manaCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        energyCap.invalidate();
        itemCap.invalidate();
        if (manaCap != null)
            manaCap.invalidate();
    }

    @Override
    public void reviveCaps()
    {
        super.reviveCaps();
        energyCap = LazyOptional.of(() -> energy);
        itemCap = LazyOptional.of(() -> outputView);
        manaCap = null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.put("Energy", energy.serializeNBT());
        tag.putInt("Mana", mana);
        tag.putInt("Progress", progress);
        tag.put("Output", output.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        if (tag.contains("Energy"))
            energy.deserializeNBT(tag.get("Energy"));
        mana = tag.getInt("Mana");
        progress = tag.getInt("Progress");
        output.deserializeNBT(tag.getCompound("Output"));
    }
}
