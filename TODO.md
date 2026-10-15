# SkyOrigins 2 Core — TODO

Order follows the Idea Court verdicts (2026-10-07). Each feature has a "prove it first" step before content work.

## 0. Housekeeping
- [x] Commit and push the `skyorigins2core` rename
- [x] Pick a license: All Rights Reserved for code and assets (`LICENSE`, `mod_license`)
- [ ] Set the CurseForge project license to match when the mod is published
- [ ] Before committing any code or art copied from other mods/tutorials, check its license allows it
- [x] Remove the MDK example block, item, creative tab and demo config
- [x] Run `.\gradlew.bat genIntellijRuns` (or `genEclipseRuns`) and confirm `runClient` launches
- [x] Decide the base modlist for 1.20.1 — `MODLIST.md`, downloaded with `tools/download_mods.py`
- [ ] Work through the open checks in `MODLIST.md` (team sync, all-mods launch test, sieve coverage)

## 1. Island Cores — GO WITH CHANGES
- [ ] **Spawn test first:** Void Island Core (`void_island` + `void_isle` biome) on a LAN/dedicated server
  - [x] Mobs actually spawn on a tiny void island (via `IslandSpawner`; tune rate/caps after more play)
  - [ ] Biome survives relog and chunk unload
  - [x] Clients see the biome change without rejoining
- [x] Custom datapack biomes that list spawns directly — `void_isle`, `desert_isle`, `crust_isle` (blazes without a fortress); reef isle needs water placement first (guardians only spawn in water)
- [x] Cores grow a random island from a weighted pool (`data/<ns>/island_pools/*.json`); Void Island Core: void ×2, desert ×1, crust ×1
- [x] Islands grow bottom-up; sand/gravel on overhanging edges becomes the base block so it can't fall
- [x] Fallback if spawn rates are too low: grown islands make their own spawn attempts (`IslandSpawner`) — needed, vanilla spawning didn't reach the island in testing
- [x] Island Core block + JSON definition per island (`data/<ns>/islands/*.json`: shape, blocks, biome, speed)
- [x] Growth spread over many ticks, with a lag cap (≤64 blocks/tick, default 8)
- [ ] Hand-built island templates (NBT) instead of the procedural disc, once the shape is proven
- [ ] Ship exactly 3 islands; cores cost processed materials (no recipe yet — creative tab / `/give` only)

## 2. Void Origins — GO WITH CHANGES
- [ ] Classic default start + 1 new origin, built with Skyblock Builder templates, NBT, loot tables and quests (no custom Java)
- [ ] New origin's first 1–3 hours must use a genuinely different resource method
- [ ] Playtest it end to end; cut the feature if it isn't clearly fun and different
- [ ] Only then add a second origin
- [ ] Cheap respec item or command
- [ ] Frame as "starts", not deep Origins-mod-style races

## 3. Arcane-Tech Bridge — GO WITH CHANGES (tier 1 only)
- [x] Register 3 items: Void Dust, Arcane Alloy, Resonant Circuit (tagged `forge:dusts/void`, `forge:ingots/arcane_alloy`)
- [x] Add the tech/magic mods to the dev environment (`devMods` in `build.gradle`, jars from `pack-mods/`): KubeJS, Ex Deorum, Botania, Mekanism, JEI
- [ ] Add Create to the dev environment (needs Flywheel and Ponder as separate dev dependencies)
- [x] First KubeJS recipes (`kubejs/server_scripts/arcane_tech_bridge.js`): Void Dust from sieving dust (5–30% by mesh), Arcane Alloy from a mana pool (3000 mana), Resonant Circuit from a Metallurgic Infuser (20 redstone)
- [ ] Playtest the chain in game and tune the numbers
- [x] 3–5 KubeJS crossover gates (`kubejs/server_scripts/arcane_tech_gates.js`): Metallurgic Infuser + Enrichment Chamber need Arcane Alloy; Runic Altar + Terrestrial Agglomeration Plate need a Resonant Circuit
- [ ] A quest explaining each gate (needs FTB Quests in the dev environment)
- [x] Renamed our "Infused Alloy" to Arcane Alloy (`arcane_alloy`): Mekanism already has an Infused Alloy
- [ ] Playtest with one tech-only and one magic-only player; cut any gate taking the "wrong-side" player > ~15 min
- [ ] Write down the Void Condenser's output rate vs. a sieve before building it
- [ ] Stretch: single-block Condenser (Forge Energy + Botania mana only). Ars Nouveau and multiblock after release

## 4. Multiplayer — GO WITH CHANGES (config, not code)
- [ ] Check Skyblock Builder teams and FTB Teams stay in sync (or find/write a bridge)
- [ ] Preconfigure Skyblock Builder + FTB Teams/Quests
- [ ] Later: island score built on FTB Quests completion data, datapack-configurable
- [ ] Drop the custom Sky Market; add Lightman's Currency via config if players ask

## 5. Later / after release
- [ ] Sky Events, trimmed: meteors only, landing as mini-islands in open void, no block damage, ore capped ~1 tier above current sieving. No damaging storms
- [ ] Prestige points from quest milestones, ~5 perks, same world (no reset). Store points per player so a real Ascension add-on stays possible
- [ ] Revisit Ascension only if quest-completion data shows many players finishing
