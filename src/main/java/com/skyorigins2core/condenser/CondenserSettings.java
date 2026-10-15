package com.skyorigins2core.condenser;

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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

// The Void Condenser's costs and outputs, from data/skyorigins2core/void_condenser/default.json.
// Outputs are sieve inputs (cobble, gravel, sand, dust) plus a little Void Dust, so the Condenser
// feeds sieves rather than replacing them.
public class CondenserSettings extends SimpleJsonResourceReloadListener
{
    public record Output(ResourceLocation item, int count, int weight)
    {
        static final Codec<Output> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Output::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Output::count),
                Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(Output::weight)
        ).apply(instance, Output::new));
    }

    public record Settings(int ticks, int energyPerTick, int mana, List<Output> outputs)
    {
        static final Codec<Settings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 72000).fieldOf("ticks").forGetter(Settings::ticks),
                Codec.intRange(0, 100000).fieldOf("energy_per_tick").forGetter(Settings::energyPerTick),
                Codec.intRange(0, 1000000).fieldOf("mana").forGetter(Settings::mana),
                Output.CODEC.listOf().fieldOf("outputs").forGetter(Settings::outputs)
        ).apply(instance, Settings::new));

        // Outputs whose item isn't installed (e.g. Ex Deorum missing) are skipped
        public ItemStack roll(RandomSource random)
        {
            List<Output> available = outputs.stream().filter(o -> ForgeRegistries.ITEMS.containsKey(o.item())).toList();
            int total = available.stream().mapToInt(Output::weight).sum();
            if (total <= 0)
                return ItemStack.EMPTY;
            int pick = random.nextInt(total);
            for (Output output : available)
            {
                pick -= output.weight();
                if (pick < 0)
                    return new ItemStack(ForgeRegistries.ITEMS.getValue(output.item()), output.count());
            }
            return ItemStack.EMPTY;
        }
    }

    private static final ResourceLocation DEFAULT = ResourceLocation.fromNamespaceAndPath(SkyOrigins2Core.MODID, "default");
    // Used until data loads, and if the data file is missing or invalid
    private static final Settings FALLBACK = new Settings(200, 40, 1500,
            List.of(new Output(ResourceLocation.fromNamespaceAndPath("minecraft", "cobblestone"), 4, 1)));
    private static final Gson GSON = new GsonBuilder().create();
    private static Settings current = FALLBACK;

    public CondenserSettings()
    {
        super(GSON, "void_condenser");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler)
    {
        JsonElement json = files.get(DEFAULT);
        current = json == null ? FALLBACK : Settings.CODEC.parse(JsonOps.INSTANCE, json)
                .resultOrPartial(error -> SkyOrigins2Core.LOGGER.error("Invalid Void Condenser settings: {}", error))
                .orElse(FALLBACK);
        SkyOrigins2Core.LOGGER.info("Void Condenser: {} ticks, {} FE/t, {} mana, {} outputs",
                current.ticks(), current.energyPerTick(), current.mana(), current.outputs().size());
    }

    public static Settings get()
    {
        return current;
    }
}
