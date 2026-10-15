package com.skyorigins2core.island;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

// One island type, loaded from data/<namespace>/islands/<name>.json. With a "template" the island is a
// hand-shaped structure (IslandTemplates); without one it's a procedural disc built from surface/filler/base.
// radius and depth should cover the whole island either way: the biome stamp and mob spawning use them.
public record IslandDefinition(
        int radius,
        int depth,
        Optional<ResourceLocation> template,
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
            ResourceLocation.CODEC.optionalFieldOf("template").forGetter(IslandDefinition::template),
            // Procedural shape only (ignored when there's a template)
            Codec.intRange(0, 32).optionalFieldOf("filler_depth", 2).forGetter(IslandDefinition::fillerDepth),
            ForgeRegistries.BLOCKS.getCodec().optionalFieldOf("surface", Blocks.GRASS_BLOCK).forGetter(IslandDefinition::surface),
            ForgeRegistries.BLOCKS.getCodec().optionalFieldOf("filler", Blocks.DIRT).forGetter(IslandDefinition::filler),
            ForgeRegistries.BLOCKS.getCodec().optionalFieldOf("base", Blocks.STONE).forGetter(IslandDefinition::base),
            Pool.CODEC.optionalFieldOf("pool").forGetter(IslandDefinition::pool),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(IslandDefinition::biome),
            // Growth speed, capped so a core can never place more than 64 blocks in one tick
            Codec.intRange(1, 64).optionalFieldOf("blocks_per_tick", 8).forGetter(IslandDefinition::blocksPerTick)
    ).apply(instance, IslandDefinition::new));

    // What the core's own spot becomes when growth finishes: the template's anchor block, else the pool
    // if there is one, otherwise the surface
    public BlockState coreState()
    {
        IslandTemplates.Template shape = template.map(IslandTemplates::get).orElse(null);
        if (shape != null)
            return shape.anchor();
        return pool.map(Pool::block).orElse(surface).defaultBlockState();
    }
}
