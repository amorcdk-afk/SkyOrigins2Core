package com.skyorigins2core.island;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;
import java.util.Optional;

// Vanilla natural spawning almost never hits a small floating island: it won't spawn within 24 blocks
// of a player, and it picks a random height for each attempt, so a one-block-thick surface high in the
// void is rarely chosen. Instead, each grown island makes its own attempts on its own surface, using the
// biome's mob list and vanilla's spawn checks (light, grass for animals, peaceful, game rules).
public final class IslandSpawner
{
    private static final int INTERVAL_TICKS = 20;
    // Only islands with a player this close try to spawn, so far-off islands cost nothing
    private static final double ACTIVATION_RANGE = 64.0;
    // Much closer than vanilla's 24, or nothing could ever spawn on an island the player is standing on
    private static final double MIN_PLAYER_DISTANCE = 5.0;
    // Per island and per category, so islands fill up without flooding
    private static final int CAP_PER_CATEGORY = 4;
    // How far above or below the island's top a surface can be and still count as the island
    private static final int SURFACE_RANGE = 16;
    private static final MobCategory[] CATEGORIES = { MobCategory.CREATURE, MobCategory.MONSTER };

    // Toggled by /skyorigins islands debug: shows each attempt's result on nearby players' action bars
    public static boolean debug;

    private IslandSpawner() {}

    public static void tick(ServerLevel level)
    {
        if (level.getGameTime() % INTERVAL_TICKS != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;
        List<IslandRegistry.Island> islands = IslandRegistry.get(level).islands();
        if (debug && islands.isEmpty())
            level.players().forEach(player -> player.displayClientMessage(Component.literal("No islands registered in this dimension"), true));
        for (IslandRegistry.Island island : islands)
        {
            String result = trySpawn(level, island);
            if (debug)
                report(level, island, result);
        }
    }

    private static void report(ServerLevel level, IslandRegistry.Island island, String result)
    {
        BlockPos c = island.center();
        Component message = Component.literal("Island " + c.getX() + " " + c.getY() + " " + c.getZ() + ": " + result);
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(c.getX() + 0.5, c.getY(), c.getZ() + 0.5) <= ACTIVATION_RANGE * ACTIVATION_RANGE)
                player.displayClientMessage(message, true);
    }

    // Returns what happened, for the debug display
    private static String trySpawn(ServerLevel level, IslandRegistry.Island island)
    {
        BlockPos center = island.center();
        if (!level.isLoaded(center))
            return "chunk not loaded";
        if (!level.hasNearbyAlivePlayer(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, ACTIVATION_RANGE))
            return "no player within " + (int) ACTIVATION_RANGE + " blocks";

        RandomSource random = level.random;
        MobCategory category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        String name = category.getName();
        int r = island.radius();
        AABB area = new AABB(center).inflate(r + 4, SURFACE_RANGE, r + 4);
        if (level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory() == category).size() >= CAP_PER_CATEGORY)
            return name + ": island is full";

        // A random column on the island, standing on whatever is on top of it
        BlockPos column = center.offset(random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
        if (!level.isLoaded(column))
            return name + ": column not loaded";
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (Math.abs(pos.getY() - center.getY()) > SURFACE_RANGE)
            return name + ": no island surface at " + pos.getX() + " " + pos.getZ();
        if (level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, MIN_PLAYER_DISTANCE))
            return name + ": too close to a player";

        Holder<Biome> biome = level.getBiome(pos);
        Optional<MobSpawnSettings.SpawnerData> entry = biome.value().getMobSettings().getMobs(category).getRandom(random);
        if (entry.isEmpty())
            return name + ": biome " + biome.unwrapKey().map(key -> key.location().toString()).orElse("?")
                    + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " has no mobs of this kind";
        EntityType<?> type = entry.get().type;
        String mobName = EntityType.getKey(type).getPath();
        if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random))
            return mobName + ": spawn rules failed (light or ground)";
        if (!level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)))
            return mobName + ": no room";

        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob))
        {
            if (entity != null)
                entity.discard();
            return mobName + ": not a mob";
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        // Lets other mods' spawn rules and events veto the spawn, as they would for vanilla spawning
        if (!ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.NATURAL))
        {
            mob.discard();
            return mobName + ": position check failed";
        }
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        level.addFreshEntityWithPassengers(mob);
        return "spawned " + mobName + " at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
