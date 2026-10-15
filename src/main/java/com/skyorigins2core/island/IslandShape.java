package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Works out which block goes where. The plan is deterministic for a given core position,
// so a half-grown island can be resumed after a reload by remembering only how far it got.
public final class IslandShape
{
    public record Placement(BlockPos pos, BlockState state) {}

    private IslandShape() {}

    public static List<Placement> plan(IslandDefinition definition, BlockPos core)
    {
        if (definition.template().isPresent())
        {
            IslandTemplates.Template template = IslandTemplates.get(definition.template().get());
            if (template != null)
                return planTemplate(template, core);
            SkyOrigins2Core.LOGGER.error("Island template {} isn't loaded; growing a plain island instead", definition.template().get());
        }
        int radius = definition.radius();
        int depth = definition.depth();
        long seed = core.asLong();

        // Work out every layer first, so we know which blocks will have something under them
        List<List<BlockPos>> layers = new ArrayList<>();
        Set<BlockPos> island = new HashSet<>();
        for (int layer = 0; layer < depth; layer++)
        {
            // Wide flat top tapering to a point underneath
            double layerRadius = radius * Math.pow(1.0 - (double) layer / depth, 0.7) + 0.5;
            List<BlockPos> ring = new ArrayList<>();
            for (int x = -radius; x <= radius; x++)
            {
                for (int z = -radius; z <= radius; z++)
                {
                    // Roughen the edge by up to ±0.6 blocks so islands aren't perfect discs
                    long hash = mix(seed ^ (x * 3129871L) ^ (z * 116129781L) ^ (layer * 42317861L));
                    double jitter = ((hash & 1023) / 1023.0 - 0.5) * 1.2;
                    if (Math.sqrt(x * x + z * z) <= layerRadius + jitter)
                        ring.add(core.offset(x, -layer, z));
                }
            }
            ring.sort(Comparator.comparingInt(pos -> horizontalDistSqr(pos, core)));
            layers.add(ring);
            island.addAll(ring);
        }

        // Bottom layer first, each layer growing from the centre outwards, so every block lands on the one below
        List<Placement> placements = new ArrayList<>();
        // Pool blocks go last, once the rim and floor around them exist, so water can't spill over an unfinished edge
        List<Placement> pool = new ArrayList<>();
        BlockState base = definition.base().defaultBlockState();
        for (int layer = depth - 1; layer >= 0; layer--)
        {
            BlockState state = (layer == 0 ? definition.surface()
                    : layer <= definition.fillerDepth() ? definition.filler()
                    : definition.base()).defaultBlockState();
            for (BlockPos pos : layers.get(layer))
            {
                if (pos.equals(core))
                    continue; // the core itself sits here and becomes coreBlock() when finished
                if (inPool(definition, core, pos, layer))
                {
                    pool.add(new Placement(pos, definition.pool().get().block().defaultBlockState()));
                    continue;
                }
                // Sand and gravel on an overhanging edge would fall into the void; use the base block there
                boolean unsupported = state.getBlock() instanceof FallingBlock && !island.contains(pos.below());
                placements.add(new Placement(pos, unsupported ? base : state));
            }
        }
        placements.addAll(pool);
        return placements;
    }

    // Bottom layer first, each layer from the centre outwards, so every block lands on the one below.
    // Anything holding a fluid (water, waterlogged coral) goes last, once the rims around it exist.
    private static List<Placement> planTemplate(IslandTemplates.Template template, BlockPos core)
    {
        Comparator<Placement> order = Comparator.comparingInt((Placement p) -> p.pos().getY())
                .thenComparingInt(p -> horizontalDistSqr(p.pos(), core));
        List<Placement> solid = new ArrayList<>();
        List<Placement> fluid = new ArrayList<>();
        for (Placement block : template.blocks())
            (block.state().getFluidState().isEmpty() ? solid : fluid).add(new Placement(core.offset(block.pos()), block.state()));
        solid.sort(order);
        fluid.sort(order);
        solid.addAll(fluid);
        return solid;
    }

    private static boolean inPool(IslandDefinition definition, BlockPos core, BlockPos pos, int layer)
    {
        return definition.pool()
                .map(pool -> layer < pool.depth() && horizontalDistSqr(pos, core) <= pool.radius() * pool.radius())
                .orElse(false);
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
