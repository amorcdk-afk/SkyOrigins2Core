package com.skyorigins2core.mana;

import com.skyorigins2core.SkyOrigins2Core;
import com.skyorigins2core.api.SkyOriginsCapabilities;
import com.skyorigins2core.api.mana.IPlayerMana;
import com.skyorigins2core.api.mana.ManaAttributes;
import com.skyorigins2core.network.ManaSyncPacket;
import com.skyorigins2core.network.ModNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

// Gives every player a mana pool: attaches it, regenerates it, keeps it across death and
// dimension changes, and syncs it to the player's client for the mana bar.
public final class ManaEvents
{
    private static final ResourceLocation KEY = ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "player_mana");

    private ManaEvents() {}

    public static void register(IEventBus modEventBus)
    {
        modEventBus.addListener((RegisterCapabilitiesEvent event) -> event.register(IPlayerMana.class));
        modEventBus.addListener((EntityAttributeModificationEvent event) -> {
            event.add(EntityType.PLAYER, ManaAttributes.MAX_MANA.get());
            event.add(EntityType.PLAYER, ManaAttributes.MANA_REGEN.get());
        });

        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, ManaEvents::attach);
        MinecraftForge.EVENT_BUS.addListener(ManaEvents::onClone);
        MinecraftForge.EVENT_BUS.addListener(ManaEvents::onLogin);
        MinecraftForge.EVENT_BUS.addListener(ManaEvents::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(ManaEvents::onChangeDimension);
        MinecraftForge.EVENT_BUS.addListener(ManaEvents::onPlayerTick);
    }

    public static Optional<PlayerMana> of(Player player)
    {
        return player.getCapability(SkyOriginsCapabilities.PLAYER_MANA).resolve()
                .filter(PlayerMana.class::isInstance).map(PlayerMana.class::cast);
    }

    private static void attach(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player player)
            event.addCapability(KEY, new Provider(new PlayerMana(player)));
    }

    // Death or returning from the End makes a new player object: carry the mana over
    private static void onClone(PlayerEvent.Clone event)
    {
        Player original = event.getOriginal();
        original.reviveCaps();
        of(original).ifPresent(old -> of(event.getEntity()).ifPresent(current -> current.copyFrom(old)));
        original.invalidateCaps();
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event)
    {
        applyBaseValues(event.getEntity());
        of(event.getEntity()).ifPresent(PlayerMana::forceSync);
    }

    // Respawning after death starts with full mana
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        applyBaseValues(event.getEntity());
        of(event.getEntity()).ifPresent(mana -> {
            if (!event.isEndConquered())
                mana.fill();
            mana.forceSync();
        });
    }

    private static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        of(event.getEntity()).ifPresent(PlayerMana::forceSync);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
            return;
        of(player).ifPresent(mana -> {
            mana.clampToMax();
            if (player.tickCount % 20 == 0)
                mana.regenerate(mana.getRegenPerSecond());
            if (mana.takeSyncChange())
                ModNetwork.sendTo(player, new ManaSyncPacket(mana.getMana(), mana.getMaxMana()));
        });
    }

    // Base values come from the common config; attribute modifiers from other mods stack on top
    private static void applyBaseValues(Player player)
    {
        setBase(player, ManaAttributes.MAX_MANA.get(), ManaConfig.BASE_MAX_MANA.get());
        setBase(player, ManaAttributes.MANA_REGEN.get(), ManaConfig.BASE_MANA_REGEN.get());
    }

    private static void setBase(Player player, Attribute attribute, double value)
    {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null)
            instance.setBaseValue(value);
    }

    private static final class Provider implements ICapabilitySerializable<CompoundTag>
    {
        private final PlayerMana mana;
        private final LazyOptional<IPlayerMana> optional;

        Provider(PlayerMana mana)
        {
            this.mana = mana;
            this.optional = LazyOptional.of(() -> mana);
        }

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
        {
            return cap == SkyOriginsCapabilities.PLAYER_MANA ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT()
        {
            return mana.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag)
        {
            mana.deserializeNBT(tag);
        }
    }
}
