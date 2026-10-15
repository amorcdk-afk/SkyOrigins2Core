package com.skyorigins2core.island;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads hand-shaped islands from data/<namespace>/structures/islands/*.nbt (built by tools/make_islands.py)
// on server start and /reload. They're ordinary structure files plus an "anchor": the block where the
// core sits, which the core's spot becomes when the island finishes.
public class IslandTemplates extends SimplePreparableReloadListener<Map<ResourceLocation, IslandTemplates.Template>>
{
    private static final String FOLDER = "structures/islands";
    private static Map<ResourceLocation, Template> templates = Map.of();

    // Blocks relative to the anchor (which isn't included), and the anchor's own block
    public record Template(List<IslandShape.Placement> blocks, BlockState anchor) {}

    @Override
    protected Map<ResourceLocation, Template> prepare(ResourceManager manager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, Template> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources(FOLDER, path -> path.getPath().endsWith(".nbt")).entrySet())
        {
            ResourceLocation file = entry.getKey();
            // data/ns/structures/islands/name.nbt -> ns:islands/name, the id island files use as "template"
            String path = file.getPath();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                    path.substring("structures/".length(), path.length() - ".nbt".length()));
            try (InputStream stream = entry.getValue().open())
            {
                Template template = parse(NbtIo.readCompressed(stream));
                if (template != null)
                    loaded.put(id, template);
                else
                    SkyOrigins2Core.LOGGER.error("Island template {} has no anchor or anchor block", id);
            }
            catch (IOException | RuntimeException e)
            {
                SkyOrigins2Core.LOGGER.error("Couldn't read island template {}", id, e);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<ResourceLocation, Template> loaded, ResourceManager manager, ProfilerFiller profiler)
    {
        templates = Map.copyOf(loaded);
        SkyOrigins2Core.LOGGER.info("Loaded {} island templates", templates.size());
    }

    @Nullable
    public static Template get(ResourceLocation id)
    {
        return templates.get(id);
    }

    @Nullable
    private static Template parse(CompoundTag tag)
    {
        if (!tag.contains("anchor", Tag.TAG_LIST))
            return null;
        BlockPos anchor = readPos(tag.getList("anchor", Tag.TAG_INT));

        List<BlockState> palette = new ArrayList<>();
        for (Tag entry : tag.getList("palette", Tag.TAG_COMPOUND))
            palette.add(BlockState.CODEC.parse(NbtOps.INSTANCE, entry).result().orElseThrow(
                    () -> new IllegalArgumentException("Unknown block state " + entry)));

        List<IslandShape.Placement> blocks = new ArrayList<>();
        BlockState anchorState = null;
        for (Tag entry : tag.getList("blocks", Tag.TAG_COMPOUND))
        {
            CompoundTag block = (CompoundTag) entry;
            BlockPos pos = readPos(block.getList("pos", Tag.TAG_INT));
            BlockState state = palette.get(block.getInt("state"));
            if (pos.equals(anchor))
                anchorState = state;
            else if (!state.isAir())
                blocks.add(new IslandShape.Placement(pos.subtract(anchor), state));
        }
        return anchorState == null ? null : new Template(List.copyOf(blocks), anchorState);
    }

    private static BlockPos readPos(ListTag list)
    {
        return new BlockPos(list.getInt(0), list.getInt(1), list.getInt(2));
    }
}
