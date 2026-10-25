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
- [ ] Work through the open checks in `MODLIST.md` (team sync, sieve coverage and the server launch test done; client launch test left)
- [x] Void biome (2026-10-17): Skyblock Builder 5.1.33 only filters biomes when its list matches none of a dimension's normal biomes (otherwise it logs "Skipping biome filtering" and keeps them all), so the void was a random mix. The overworld is now the new `void_sky` biome (night monsters, no animals; animals come from Void Islands) via `config/skyblockbuilder/world.json5`; the Nether keeps all its biomes (its warning is expected). Applies to new worlds
- [ ] Playtest the new sieve rates (metal floors in `tools/make_sieve_rates.py`); SkyOrigins 1's were too low, so err high

## 1. Island Cores — GO WITH CHANGES
- [ ] **Spawn test first:** Void Island Core (`void_island` + `void_isle` biome) on a LAN/dedicated server
  - [x] Mobs actually spawn on a tiny void island (via `IslandSpawner`; tune rate/caps after more play)
  - [x] Biome survives relog and chunk unload (checked 2026-10-15)
  - [x] Clients see the biome change without rejoining
- [x] Custom datapack biomes that list spawns directly — `void_isle`, `desert_isle`, `crust_isle` (blazes without a fortress), `reef_isle` (guardians and drowned in a 2-deep water pool; islands support an optional `pool`, placed last so it can't spill)
- [x] Cores grow a random island from a weighted pool (`data/<ns>/island_pools/*.json`); Void Island Core: void ×2, desert ×1, crust ×1, reef ×1
- [x] Islands grow bottom-up; sand/gravel on overhanging edges becomes the base block so it can't fall
- [x] Fallback if spawn rates are too low: grown islands make their own spawn attempts (`IslandSpawner`) — needed, vanilla spawning didn't reach the island in testing
- [x] Island Core block + JSON definition per island (`data/<ns>/islands/*.json`: shape, blocks, biome, speed)
- [x] Growth spread over many ticks, with a lag cap (≤64 blocks/tick, default 8)
- [x] Hand-shaped island templates instead of the procedural disc: `tools/make_islands.py` builds `structures/islands/*.nbt` (void: oak tree and mossy boulder; desert: cacti and a broken pillar; crust: huge crimson fungus; reef: sunken pool with sea life); island JSONs point at them with `template`, the disc remains as a fallback; preview with `tools/render_structure.py`; GameTest checks every template loads with nothing that could fall
- [x] In game: grow each island from a Void Island Core and check it looks right and the reef pool holds (checked in game 2026-10-15)
- [x] Ship exactly 3 islands; cores cost processed materials — Void Island Core: 4 Void Dust + 4 dirt + 1 Arcane Alloy (`kubejs/server_scripts/island_cores.js`)
- [x] Island Cores quest chapter (`ftbquests/quests/chapters/island_cores.snbt`, 6 quests): the core recipe and odds, a visit-the-biome quest per island with its mobs and loot, and Archipelago for all four; unlocks from Arcane Alloy
- [ ] Check the Island Cores chapter in game
- [x] GameTests (`ModGameTests`, run with `gradlew runGameTestServer`): core grows and registers; condenser produces output; reef pool stays inside its rim

## 2. Void Origins — GO WITH CHANGES
- [x] Skyblock Builder (+ LibX) in the dev environment; its default configs are in `run/config/skyblockbuilder/`
- [x] Picked the starts (Idea Court, 2026-10-15; Void Fisher, Wreck Salvager, Crust Exile ruled out, Void Fisher is the fallback):
  main start **SkyFactory** (one oak on one grass block), option **Botanist's Grove** (GO WITH CHANGES)
- [x] Both templates built by `tools/make_templates.py` into `config/skyblockbuilder/templates/`; `config/` is copied into `run/config` before runs; players may create their own team (`createOwnTeam`) so servers can pick a start
- [x] In game: create a Skyblock world with each start and check the island, spawn point and Grove chest (checked in game 2026-10-15)
- [x] Grove bootstrap (mana makes them, `kubejs/server_scripts/botanists_grove.js`): Pure Daisy dirt -> stone (2x normal time); mana infusion stone -> copper ore chunk (1,000), cobblestone -> iron ore chunk (1,500); the chest has 1 copper ingot for the first spreader and a water bucket for the apothecary
- [ ] Grove rules from the verdict: no recipe removals or nerfs; new mana recipes priced at or above sieving so the SkyFactory start gains no shortcut; normal Orechid only
- [x] Grove first-hour quest chapter (`ftbquests/quests/chapters/botanists_grove.snbt`, 9 quests)
- [ ] Playtest the Grove mana prices against the SkyFactory start (does a classic player with a cobble generator gain anything?)
- [x] SkyFactory Start quest chapter (`ftbquests/quests/chapters/skyfactory_start.snbt`, 15 quests: tree -> crook -> barrel -> silk worm/string -> sieve -> cobblestone -> hammer/dust -> clay -> porcelain crucible -> cobblestone generator); chapter order SkyFactory, Grove, Arcane-Tech Bridge
- [x] Grove kill test: cut it if hour one is mostly waiting on mana, or it merges into sieving before ~90 min (passed: first hour checked in game 2026-10-15)
- [ ] New origin's first 1–3 hours must use a genuinely different resource method
- [ ] Playtest it end to end; cut the feature if it isn't clearly fun and different
- [ ] Only then add a second origin
- [ ] Cheap respec item or command
- [ ] Frame as "starts", not deep Origins-mod-style races

## 3. Arcane-Tech Bridge — GO WITH CHANGES (tier 1 only)
- [x] Register 3 items: Void Dust, Arcane Alloy, Resonant Circuit (tagged `forge:dusts/void`, `forge:ingots/arcane_alloy`)
- [x] Add the tech/magic mods to the dev environment (`devMods` in `build.gradle`, jars from `pack-mods/`): KubeJS, Ex Deorum, Botania, Mekanism, JEI
- [x] Add Create to the dev environment (2026-10-17): `tools/split_jarjar.py` pulls Flywheel, Ponder and Registrate out of Create's jar into pack-mods/ and writes `create_dev-*.jar` without them; dev server (6/6 GameTests) and dev client (title screen, Flywheel shaders) both load
- [x] Create content (2026-10-17): Whisk gate (Arcane Alloy -> Mechanical Mixer -> brass), Void Dust from crushing dust (12%), Resonant Circuit from heated mixing (Arcane Alloy + 2 redstone, same cost as the Infuser); Bridge chapter has a Whisk gate quest and the Resonant Circuit quest accepts either route
- [ ] Playtest the Create route (does gating the Whisk hold Create back the right amount?)
- [x] First KubeJS recipes (`kubejs/server_scripts/arcane_tech_bridge.js`): Void Dust from sieving dust (5–30% by mesh), Arcane Alloy from a mana pool (3000 mana), Resonant Circuit from a Metallurgic Infuser (20 redstone)
- [x] Playtest the chain in game and tune the numbers (played through, numbers fine, 2026-10-15)
- [x] 3–5 KubeJS crossover gates (`kubejs/server_scripts/arcane_tech_gates.js`): Metallurgic Infuser + Enrichment Chamber need Arcane Alloy; Runic Altar + Terrestrial Agglomeration Plate need a Resonant Circuit
- [x] FTB Quests, FTB Teams and FTB Library in the dev environment; quests live in `ftbquests/quests/` (linked into `run/config/ftbquests/quests`, so in-game edits save to the repo)
- [x] A quest explaining each gate: "Arcane-Tech Bridge" chapter (Void Dust -> Arcane Alloy -> 4 gates, Resonant Circuit, Void Condenser, Void Island Core)
- [x] Check the chapter in game (layout, wording) and tune the XP rewards (checked in game 2026-10-15)
- [x] Renamed our "Infused Alloy" to Arcane Alloy (`arcane_alloy`): Mekanism already has an Infused Alloy
- [ ] Playtest with one tech-only and one magic-only player; cut any gate taking the "wrong-side" player > ~15 min
- [x] Write down the Void Condenser's output rate vs. a sieve before building it — makes sieve *inputs*, not ores: 1 roll per 10 s (8,000 FE + 1,500 mana) ≈ 15 blocks/min, ~1 Void Dust per 3 min; tunable in `data/skyorigins2core/void_condenser/default.json`
- [x] Single-block Void Condenser (Forge Energy + Botania mana spreader input, 9-slot extract-only buffer); recipe in `kubejs/server_scripts/void_condenser.js`
- [ ] Ars Nouveau (Source) input and multiblock version — after release

## 4. Multiplayer — GO WITH CHANGES (config, not code)
- [x] Check Skyblock Builder teams and FTB Teams stay in sync: they don't (2026-10-15). Skyblock Builder 5.1.33 has no FTB compat, so island teammates keep separate FTB Quests progress
- [x] Bridge them: `compat/TeamSyncCompat` mirrors each Skyblock Builder island team into an FTB Teams party (once a second and on login; Skyblock Builder is the source of truth)
- [x] Test the bridge with two players (LAN or `runServer` + two clients): create an island team, invite, accept, check `/ftbteams info` and shared quest progress; leave and check the party drops you; owner leaving hands the party on (checked in game 2026-10-15)
- [ ] Preconfigure Skyblock Builder + FTB Teams/Quests
- [ ] Later: island score built on FTB Quests completion data, datapack-configurable
- [ ] Drop the custom Sky Market; add Lightman's Currency via config if players ask

## API systems for other mods (see `API.md`)
- [x] Player mana: per-player pool, regen, `max_mana` / `mana_regen` attributes, XP-style blue mana bar right of the hotbar, labelled "Mana x/y", `/skyorigins mana`
- [x] Void Charge energy API (1 VC = 4 FE): capability, `VoidChargeStorage`, `VoidCharge` helpers — API only, no machines in this mod (decided 2026-10-15)
- [x] Publish to local maven with `gradlew publish` (`mcmodsrepo/`, artifact `com.skyorigins2core:skyorigins2core`)
- [x] In game: mana bar sits correctly with hearts/armor/hunger and survives death and relog (checked 2026-10-15)

## New mods (added 2026-10-18)
- [x] Added AE2, Storage Drawers, Sophisticated Backpacks, Farmer's Delight, Industrial Foregoing, Mystical Agradditions, FTB Chunks, FTB Ultimine and QoL/performance mods (see `MODLIST.md`); full-pack server starts with 54 jars, 0 KubeJS errors, 0 failed recipes
- [x] AE2 Inscriber Presses craftable (Resonant Circuit); Farmer's Delight crops from sieving dirt
- [ ] End access: there's no stronghold in a void world, so no End portal (Mystical Agradditions' dragon egg chunks, elytra, shulkers). Decide: craftable End Portal Frames, or a quest reward
- [x] FTB Chunks defaults reviewed (2026-10-18): kept as shipped (500 claims and 25 force-loaded chunks per team, protection for allies only, explosions off in claims); islands are 8,192 blocks apart so limits never collide. Teams claim their island themselves
- [x] Cleanup: removed the Forge template files (README.txt, changelog.txt, CREDITS.txt, Forge's LGPL LICENSE.txt), dev KubeJS scripts are synced (no stale example.js), Forge's mod update check is off for the pack (`config/fml.toml`)
- [ ] Quests for the new mods (AE2 presses, Farmer's Delight crops, Industrial Foregoing)
- [ ] Watch Industrial Foregoing's ore laser drill against the sieve rates

## 5. Later / after release
- [ ] Sky Events, trimmed: meteors only, landing as mini-islands in open void, no block damage, ore capped ~1 tier above current sieving. No damaging storms
- [ ] Prestige points from quest milestones, ~5 perks, same world (no reset). Store points per player so a real Ascension add-on stays possible
- [ ] Revisit Ascension only if quest-completion data shows many players finishing
