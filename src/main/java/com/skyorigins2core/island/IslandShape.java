package com.skyorigins2core.island;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Works out which block goes where. The plan is deterministic for a given core position,
// so a half-grown island can be resumed after a reload by remembering only how far it got.
public final class IslandShape
{
    public record Placement(BlockPos pos, BlockState state) {}

    private IslandShape() {}

    public static List<Placement> plan(IslandDefinition definition, BlockPos core)
    {
        List<Placement> placements = new ArrayList<>();
        int radius = definition.radius();
        int depth = definition.depth();
        long seed = core.asLong();

        // Top layer first, each layer growing from the centre outwards
        for (int layer = 0; layer < depth; layer++)
        {
            // Wide flat top tapering to a point underneath
            double layerRadius = radius * Math.pow(1.0 - (double) layer / depth, 0.7) + 0.5;
            BlockState state = (layer == 0 ? definition.surface()
                    : layer <= definition.fillerDepth() ? definition.filler()
                    : definition.base()).defaultBlockState();

            List<Placement> ring = new ArrayList<>();
            for (int x = -radius; x <= radius; x++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    if (layer == 0 && x == 0 && z == 0)
                        continue; // the core itself sits here and becomes surface when finished

                    // Roughen the edge by up to ±0.6 blocks so islands aren't perfect discs
                    long hash = mix(seed ^ (x * 3129871L) ^ (z * 116129781L) ^ (layer * 42317861L));
                    double jitter = ((hash & 1023) / 1023.0 - 0.5) * 1.2;
                    if (Math.sqrt(x * x + z * z) <= layerRadius + jitter)
                        ring.add(new Placement(core.offset(x, -layer, z), state));
                }
            }
            ring.sort(Comparator.comparingInt(p -> horizontalDistSqr(p.pos(), core)));
            placements.addAll(ring);
        }
        return placements;
    }

    // SplitMix64 finaliser: spreads nearby inputs into unrelated-looking outputs
    private static long mix(long value)
    {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static int horizontalDistSqr(BlockPos a, BlockPos b)
    {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}
