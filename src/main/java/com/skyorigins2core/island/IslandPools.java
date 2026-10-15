package com.skyorigins2core.island;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Weighted lists of islands a core can grow, loaded from data/<namespace>/island_pools/*.json
public class IslandPools extends SimpleJsonResourceReloadListener
{
    public record Entry(ResourceLocation island, int weight)
    {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("island").forGetter(Entry::island),
                Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(Entry::weight)
        ).apply(instance, Entry::new));
    }

    public record Pool(List<Entry> islands)
    {
        static final Codec<Pool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("islands").forGetter(Pool::islands)
        ).apply(instance, Pool::new));

        @Nullable
        public ResourceLocation pick(RandomSource random)
        {
            int total = islands.stream().mapToInt(Entry::weight).sum();
            if (total <= 0)
                return null;
            int roll = random.nextInt(total);
            for (Entry entry : islands)
            {
                roll -= entry.weight();
                if (roll < 0)
                    return entry.island();
            }
            return null;
        }
    }

    private static final Gson GSON = new GsonBuilder().create();
    private static Map<ResourceLocation, Pool> pools = Map.of();

    public IslandPools()
    {
        super(GSON, "island_pools");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, Pool> loaded = new HashMap<>();
        files.forEach((id, json) -> Pool.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> SkyOrigins2Core.LOGGER.error("Invalid island pool {}: {}", id, error))
                .ifPresent(pool -> loaded.put(id, pool)));
        pools = Map.copyOf(loaded);
        SkyOrigins2Core.LOGGER.info("Loaded {} island pools", pools.size());
    }

    @Nullable
    public static Pool get(ResourceLocation id)
    {
        return pools.get(id);
    }
}
