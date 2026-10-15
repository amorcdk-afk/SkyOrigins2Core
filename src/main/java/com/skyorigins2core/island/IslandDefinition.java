package com.skyorigins2core.island;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

// One island type, loaded from data/<namespace>/islands/<name>.json
public record IslandDefinition(
        int radius,
        int depth,
        int fillerDepth,
        Block surface,
        Block filler,
        Block base,
        Optional<Pool> pool,
        Optional<ResourceKey<Biome>> biome,
        int blocksPerTick)
{
    // A pool sunk into the top of the island, e.g. water for a reef. Radius is measured from the centre,
    // depth counts down from the top layer; the island's own blocks form its rim and floor.
    public record Pool(Block block, int radius, int depth)
    {
        static final Codec<Pool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ForgeRegistries.BLOCKS.getCodec().fieldOf("block").forGetter(Pool::block),
                Codec.intRange(0, 15).fieldOf("radius").forGetter(Pool::radius),
                Codec.intRange(1, 8).optionalFieldOf("depth", 2).forGetter(Pool::depth)
        ).apply(instance, Pool::new));
    }

    public static final Codec<IslandDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("radius").forGetter(IslandDefinition::radius),
            Codec.intRange(1, 32).fieldOf("depth").forGetter(IslandDefinition::depth),
            Codec.intRange(0, 32).optionalFieldOf("filler_depth", 2).forGetter(IslandDefinition::fillerDepth),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("surface").forGetter(IslandDefinition::surface),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("filler").forGetter(IslandDefinition::filler),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("base").forGetter(IslandDefinition::base),
            Pool.CODEC.optionalFieldOf("pool").forGetter(IslandDefinition::pool),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(IslandDefinition::biome),
            // Growth speed, capped so a core can never place more than 64 blocks in one tick
            Codec.intRange(1, 64).optionalFieldOf("blocks_per_tick", 8).forGetter(IslandDefinition::blocksPerTick)
    ).apply(instance, IslandDefinition::new));

    // What the core's own spot becomes when growth finishes: the pool if there is one, otherwise the surface
    public Block coreBlock()
    {
        return pool.map(Pool::block).orElse(surface);
    }
}
