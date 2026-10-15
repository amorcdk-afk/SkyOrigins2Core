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
        Optional<ResourceKey<Biome>> biome,
        int blocksPerTick)
{
    public static final Codec<IslandDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 16).fieldOf("radius").forGetter(IslandDefinition::radius),
            Codec.intRange(1, 32).fieldOf("depth").forGetter(IslandDefinition::depth),
            Codec.intRange(0, 32).optionalFieldOf("filler_depth", 2).forGetter(IslandDefinition::fillerDepth),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("surface").forGetter(IslandDefinition::surface),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("filler").forGetter(IslandDefinition::filler),
            ForgeRegistries.BLOCKS.getCodec().fieldOf("base").forGetter(IslandDefinition::base),
            ResourceKey.codec(Registries.BIOME).optionalFieldOf("biome").forGetter(IslandDefinition::biome),
            // Growth speed, capped so a core can never place more than 64 blocks in one tick
            Codec.intRange(1, 64).optionalFieldOf("blocks_per_tick", 8).forGetter(IslandDefinition::blocksPerTick)
    ).apply(instance, IslandDefinition::new));
}
