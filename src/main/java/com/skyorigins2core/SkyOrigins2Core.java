package com.skyorigins2core;

import com.mojang.logging.LogUtils;
import com.skyorigins2core.condenser.CondenserSettings;
import com.skyorigins2core.condenser.VoidCondenserBlock;
import com.skyorigins2core.condenser.VoidCondenserBlockEntity;
import com.skyorigins2core.island.IslandCommands;
import com.skyorigins2core.island.IslandCoreBlock;
import com.skyorigins2core.island.IslandCoreBlockEntity;
import com.skyorigins2core.island.IslandDefinitions;
import com.skyorigins2core.island.IslandPools;
import com.skyorigins2core.island.IslandSpawner;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SkyOrigins2Core.MODID)
public class SkyOrigins2Core
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "skyorigins2core";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "skyorigins2core" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "skyorigins2core" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold BlockEntityTypes which will all be registered under the "skyorigins2core" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "skyorigins2core" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Grows a random island from data/skyorigins2core/island_pools/void_island_core.json.
    // Unbreakable while growing, so it can't be picked back up to re-grow the same island for free.
    public static final RegistryObject<Block> VOID_ISLAND_CORE = BLOCKS.register("void_island_core",
            () -> new IslandCoreBlock(ResourceLocation.fromNamespaceAndPath(MODID, "void_island_core"),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0F, 3600000.0F)
                            .noLootTable()
                            .lightLevel(state -> 7)
                            .noOcclusion() // open frame model: don't hide neighbouring blocks' faces
                            .sound(SoundType.AMETHYST)));
    public static final RegistryObject<Item> VOID_ISLAND_CORE_ITEM = ITEMS.register("void_island_core",
            () -> new BlockItem(VOID_ISLAND_CORE.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<IslandCoreBlockEntity>> ISLAND_CORE = BLOCK_ENTITY_TYPES.register("island_core",
            () -> BlockEntityType.Builder.of(IslandCoreBlockEntity::new, VOID_ISLAND_CORE.get()).build(null));

    // Arcane-Tech Bridge materials: the tech and magic mods' key recipes need each other's parts.
    // Their recipes and gates live in KubeJS so they can be rebalanced without recompiling.
    // Void Dust comes from sieving; tagged forge:dusts/void
    public static final RegistryObject<Item> VOID_DUST = ITEMS.register("void_dust", () -> new Item(new Item.Properties()));
    // Magic-infused metal; tagged forge:ingots/arcane_alloy
    public static final RegistryObject<Item> ARCANE_ALLOY = ITEMS.register("arcane_alloy", () -> new Item(new Item.Properties()));
    // Machine-made part that magic recipes need
    public static final RegistryObject<Item> RESONANT_CIRCUIT = ITEMS.register("resonant_circuit", () -> new Item(new Item.Properties()));

    // Void Condenser: Forge Energy + Botania mana -> sieve inputs; costs and outputs in data/skyorigins2core/void_condenser/
    public static final RegistryObject<Block> VOID_CONDENSER = BLOCKS.register("void_condenser",
            () -> new VoidCondenserBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .lightLevel(state -> 4)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Item> VOID_CONDENSER_ITEM = ITEMS.register("void_condenser",
            () -> new BlockItem(VOID_CONDENSER.get(), new Item.Properties()));
    public static final RegistryObject<BlockEntityType<VoidCondenserBlockEntity>> VOID_CONDENSER_ENTITY = BLOCK_ENTITY_TYPES.register("void_condenser",
            () -> BlockEntityType.Builder.of(VoidCondenserBlockEntity::new, VOID_CONDENSER.get()).build(null));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.skyorigins2core"))
            .icon(() -> VOID_ISLAND_CORE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(VOID_ISLAND_CORE_ITEM.get());
                output.accept(VOID_DUST.get());
                output.accept(ARCANE_ALLOY.get());
                output.accept(RESONANT_CIRCUIT.get());
                output.accept(VOID_CONDENSER_ITEM.get());
            })
            .build());

    public SkyOrigins2Core(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the Deferred Registers to the mod event bus so their entries get registered
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Load island definitions from data packs on server start and /reload
        MinecraftForge.EVENT_BUS.addListener((AddReloadListenerEvent event) -> {
            event.addListener(new IslandDefinitions());
            event.addListener(new IslandPools());
            event.addListener(new CondenserSettings());
        });
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> IslandCommands.register(event.getDispatcher()));
        // Spawn mobs on grown islands, which vanilla spawning almost never reaches
        MinecraftForge.EVENT_BUS.addListener((TickEvent.LevelTickEvent event) -> {
            if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level)
                IslandSpawner.tick(level);
        });
    }
}
