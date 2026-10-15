package com.skyorigins2core.island;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;

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

    private IslandSpawner() {}

    public static void tick(ServerLevel level)
    {
        if (level.getGameTime() % INTERVAL_TICKS != 0 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))
            return;
        for (IslandRegistry.Island island : IslandRegistry.get(level).islands())
            trySpawn(level, island);
    }

    private static void trySpawn(ServerLevel level, IslandRegistry.Island island)
    {
        BlockPos center = island.center();
        if (!level.isLoaded(center)
                || !level.hasNearbyAlivePlayer(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, ACTIVATION_RANGE))
            return;

        RandomSource random = level.random;
        MobCategory category = CATEGORIES[random.nextInt(CATEGORIES.length)];
        int r = island.radius();
        AABB area = new AABB(center).inflate(r + 4, SURFACE_RANGE, r + 4);
        if (level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory() == category).size() >= CAP_PER_CATEGORY)
            return;

        // A random column on the island, standing on whatever is on top of it
        BlockPos column = center.offset(random.nextInt(2 * r + 1) - r, 0, random.nextInt(2 * r + 1) - r);
        if (!level.isLoaded(column))
            return;
        BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
        if (Math.abs(pos.getY() - center.getY()) > SURFACE_RANGE
                || level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, MIN_PLAYER_DISTANCE))
            return;

        Optional<MobSpawnSettings.SpawnerData> entry = level.getBiome(pos).value().getMobSettings().getMobs(category).getRandom(random);
        if (entry.isEmpty())
            return;
        EntityType<?> type = entry.get().type;
        if (!SpawnPlacements.checkSpawnRules(type, level, MobSpawnType.NATURAL, pos, random)
                || !level.noCollision(type.getAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)))
            return;

        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob))
        {
            if (entity != null)
                entity.discard();
            return;
        }
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
        // Lets other mods' spawn rules and events veto the spawn, as they would for vanilla spawning
        if (!ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.NATURAL))
        {
            mob.discard();
            return;
        }
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        level.addFreshEntityWithPassengers(mob);
    }
}
