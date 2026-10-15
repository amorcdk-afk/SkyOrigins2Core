package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Sets the biome over an island, the same way /fillbiome does: rewrite the chunk's biome data,
// mark it for saving, and resend it to clients so they see the change without rejoining.
public final class BiomeStamper
{
    // Mobs spawn in the air above the surface, so the biome has to cover that space too
    private static final int HEADROOM = 16;
    // Size of one biome cell in blocks
    private static final int CELL = 4;

    private BiomeStamper() {}

    public static void stamp(ServerLevel level, BlockPos core, IslandDefinition definition)
    {
        if (definition.biome().isEmpty())
            return;
        ResourceKey<Biome> key = definition.biome().get();
        Optional<? extends Holder<Biome>> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(key);
        if (biome.isEmpty())
        {
            SkyOrigins2Core.LOGGER.warn("Island biome {} does not exist", key.location());
            return;
        }
        Holder<Biome> islandBiome = biome.get();

        // Biomes are stored per 4x4x4 cell, and the game's biome lookup can read a neighbouring cell
        // ("biome fuzzing"), so cover every cell the island touches plus one more cell all round
        int r = definition.radius() + CELL;
        BoundingBox area = new BoundingBox(
                core.getX() - r, core.getY() - definition.depth() - CELL, core.getZ() - r,
                core.getX() + r, core.getY() + HEADROOM + CELL, core.getZ() + r);
        int minQx = QuartPos.fromBlock(area.minX()), maxQx = QuartPos.fromBlock(area.maxX());
        int minQy = QuartPos.fromBlock(area.minY()), maxQy = QuartPos.fromBlock(area.maxY());
        int minQz = QuartPos.fromBlock(area.minZ()), maxQz = QuartPos.fromBlock(area.maxZ());

        List<ChunkAccess> chunks = new ArrayList<>();
        for (int cx = SectionPos.blockToSectionCoord(area.minX()); cx <= SectionPos.blockToSectionCoord(area.maxX()); cx++)
        {
            for (int cz = SectionPos.blockToSectionCoord(area.minZ()); cz <= SectionPos.blockToSectionCoord(area.maxZ()); cz++)
            {
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (chunk != null)
                    chunks.add(chunk);
            }
        }

        for (ChunkAccess chunk : chunks)
        {
            chunk.fillBiomesFromNoise((qx, qy, qz, sampler) ->
                    qx >= minQx && qx <= maxQx && qy >= minQy && qy <= maxQy && qz >= minQz && qz <= maxQz
                            ? islandBiome
                            : chunk.getNoiseBiome(qx, qy, qz),
                    level.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(chunks);
    }
}
