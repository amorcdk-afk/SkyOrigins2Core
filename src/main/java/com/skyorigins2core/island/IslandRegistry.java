package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Every island grown by a core in this dimension, saved with the world so IslandSpawner can find them
public class IslandRegistry extends SavedData
{
    private static final String NAME = SkyOrigins2Core.MODID + "_islands";

    public record Island(BlockPos center, int radius) {}

    private final List<Island> islands = new ArrayList<>();

    public static IslandRegistry get(ServerLevel level)
    {
        return level.getDataStorage().computeIfAbsent(IslandRegistry::load, IslandRegistry::new, NAME);
    }

    public List<Island> islands()
    {
        return Collections.unmodifiableList(islands);
    }

    public void add(BlockPos center, int radius)
    {
        islands.add(new Island(center.immutable(), radius));
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag list = new ListTag();
        for (Island island : islands)
        {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Center", island.center().asLong());
            entry.putInt("Radius", island.radius());
            list.add(entry);
        }
        tag.put("Islands", list);
        return tag;
    }

    private static IslandRegistry load(CompoundTag tag)
    {
        IslandRegistry registry = new IslandRegistry();
        for (Tag element : tag.getList("Islands", Tag.TAG_COMPOUND))
        {
            CompoundTag entry = (CompoundTag) element;
            registry.islands.add(new Island(BlockPos.of(entry.getLong("Center")), entry.getInt("Radius")));
        }
        return registry;
    }
}
