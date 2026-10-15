package com.skyorigins2core.island;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

// Loads every data/<namespace>/islands/*.json file on server start and on /reload
public class IslandDefinitions extends SimpleJsonResourceReloadListener
{
    private static final Gson GSON = new GsonBuilder().create();
    private static Map<ResourceLocation, IslandDefinition> islands = Map.of();

    public IslandDefinitions()
    {
        super(GSON, "islands");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        Map<ResourceLocation, IslandDefinition> loaded = new HashMap<>();
        files.forEach((id, json) -> IslandDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> SkyOrigins2Core.LOGGER.error("Invalid island definition {}: {}", id, error))
                .ifPresent(definition -> loaded.put(id, definition)));
        islands = Map.copyOf(loaded);
        SkyOrigins2Core.LOGGER.info("Loaded {} island definitions", islands.size());
    }

    @Nullable
    public static IslandDefinition get(ResourceLocation id)
    {
        return islands.get(id);
    }
}
