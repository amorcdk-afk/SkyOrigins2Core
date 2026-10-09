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
- [ ] **Spawn test first:** one throwaway island + one custom biome on a LAN/dedicated server
  - [ ] Mobs actually spawn on a tiny void island at an acceptable rate
  - [ ] Biome survives relog and chunk unload
  - [ ] Clients see the biome change without rejoining
- [ ] Use custom datapack biomes (e.g. `skyorigins2core:reef_isle`) that list spawns directly — vanilla biomes won't give guardians/blazes (those are structure spawns)
- [ ] Fallback if spawn rates are too low: core makes local spawn attempts on its own island
- [ ] Island Core item + JSON definition per island (template, biome, loot)
- [ ] Growth spread over many ticks, with a lag cap
- [ ] Ship exactly 3 islands; cores cost processed materials

## 2. Void Origins — GO WITH CHANGES
- [ ] Classic default start + 1 new origin, built with Skyblock Builder templates, NBT, loot tables and quests (no custom Java)
- [ ] New origin's first 1–3 hours must use a genuinely different resource method
- [ ] Playtest it end to end; cut the feature if it isn't clearly fun and different
- [ ] Only then add a second origin
- [ ] Cheap respec item or command
- [ ] Frame as "starts", not deep Origins-mod-style races

## 3. Arcane-Tech Bridge — GO WITH CHANGES (tier 1 only)
- [ ] Register 3 items: Void Dust, Infused Alloy, Resonant Circuit
- [ ] 3–5 KubeJS crossover gates, each introduced by a quest
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
