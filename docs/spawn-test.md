# Island Core spawn test

Throwaway test from TODO §1. It answers three questions before any Island Core code is written:

1. Do mobs spawn on a tiny void island at an acceptable rate?
2. Does a custom biome survive relog, chunk unload and server restart?
3. Do clients see a biome change without rejoining?

## What ships for the test

- Biome `skyorigins2core:spawn_test`
  (`src/main/resources/data/skyorigins2core/worldgen/biome/spawn_test.json`). It lists zombie, skeleton, blaze and guardian as monsters, and sheep as a creature. Blaze and guardian are there on purpose: in vanilla they only come from structure spawns, so this shows whether a plain biome spawn list is enough.
- Functions under `skyorigins2core:spawntest/`:
  - `build`: makes a 16x16 island exactly on chunk (64, 64) (x/z 1024–1039, top at y 64), with a 4x4x3 water pool. It sets the island's biome with `/fillbiome`, builds a glass viewing platform about 35 blocks south, and teleports you there.
  - `start` / `stop`: begin and end counting. While running it counts each new mob on the island once a second and shows the totals in the sidebar.
  - `reset`: zero the counters and clear the island.
  - Auto-kill is on by default, so the local mob cap never fills. To watch mobs pile up instead, run `/scoreboard players set #autokill so2c_spawns 0`.

Remove the biome and functions once the test is done; they are not meant to ship.

## Setup

Use a dedicated server for the real run (`.\gradlew.bat runServer`), or Open to LAN from `runClient`.

1. Create a new world: Superflat, preset **The Void**. The void biome spawns nothing, so the island is the only place anything can spawn. Set difficulty to Normal or higher.
2. Join as an op and run:
   ```
   /gamerule doDaylightCycle false
   /time set midnight
   /function skyorigins2core:spawntest/build
   /function skyorigins2core:spawntest/start
   ```
3. Stay on the glass platform. Spawns need a player 24–128 blocks away, and the island chunk must be within simulation distance. Creative mode is fine; spectator mode stops spawning.

## Checks

### A. Spawn rate
Let it run for **10 minutes** (600 s on the sidebar), then `/function skyorigins2core:spawntest/stop`. Record the counts.

For comparison, repeat once with the vanilla biome, then rerun `start`:
`/fillbiome 1024 48 1024 1039 111 1039 minecraft:plains`
Then put it back:
`/fillbiome 1024 48 1024 1039 111 1039 skyorigins2core:spawn_test`

Natural spawning only picks a random Y between the bottom of the world and the top block of each column. So on a 4-block-thick island, most attempts land in the air under it. Expect low numbers; the question is whether they are *too* low. If they are, the TODO's fallback applies: the core makes its own local spawn attempts.

### B. Biome persistence
Press F3 on the island and confirm the biome shows `skyorigins2core:spawn_test`, then:
- [ ] Disconnect and rejoin → still `spawn_test`
- [ ] `/tp @s 5000 100 5000`, wait about 10 s for the chunk to unload, `/tp @s 1031 65 1074` → still `spawn_test`
- [ ] Stop and restart the server, rejoin → still `spawn_test`

### C. Client sync without rejoining
With a **second client** connected and looking at the island (F3 open, or watch the grass and water colour):
- [ ] Run the `/fillbiome ... minecraft:plains` command above → the second client's F3 biome and grass colour change without rejoining
- [ ] Switch it back to `spawn_test` → it changes back

Note: `/fillbiome` resends the chunk's biomes to clients itself. When the Island Core sets the biome from Java, it must send `ClientboundChunksBiomesPacket` itself. So re-run check C against the real core later.

## Results

| Date | Biome | Seconds | Zombie | Skeleton | Blaze | Guardian | Sheep | Other | Notes |
|------|-------|---------|--------|----------|-------|----------|-------|-------|-------|
|      | spawn_test | 600 | | | | | | | |
|      | minecraft:plains | 600 | | | | | | | |

| Check | Pass? | Notes |
|-------|-------|-------|
| Survives relog | | |
| Survives chunk unload | | |
| Survives server restart | | |
| Client sees change without rejoin | | |

**Verdict:** _(spawn rate acceptable? need the local-spawn fallback?)_
