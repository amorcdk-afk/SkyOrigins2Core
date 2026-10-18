# SkyOrigins 2 — Mod List (Forge 1.20.1)

Built from SkyOrigins 1's mod list plus the Idea Court verdicts (2026-10-07).
Versions are the newest Forge 1.20.1 files on CurseForge, downloaded and checksum-verified by
`tools/download_mods.py` (into the git-ignored `pack-mods/` folder).

## Skyblock core
| Mod | Version | Why |
|---|---|---|
| Ex Deorum | 1.53 | **Replaces Ex Nihilo: Sequentia**, whose 1.20.1 builds are NeoForge-only. Sieves, hammers, crucibles, watering can. Use Skyblock Builder for world gen, not Ex Deorum's own skyblock world type |
| Ex Machinis: Divitiae Deorum | 3.1.0 | RF-powered sieve/hammer/compactor for Ex Deorum. Not the similarly named "Ex Machinis", which needs Sequentia |
| Skyblock Builder | 5.1.33 | Team islands, spawn spacing, island templates. Backbone for Void Origins (selectable start templates) and multiplayer |
| Sky GUIs | 3.0.16 | GUI add-on for Skyblock Builder teams |

## Tech
| Mod | Version | Why |
|---|---|---|
| Create | 6.0.8 | Kept from SO1. Tech side of the Arcane-Tech Bridge |
| Mekanism, Generators, Tools | 10.4.16.80 | Kept from SO1. Tech side of the Bridge |
| Thermal Foundation | 11.0.6.70 | Kept from SO1 |
| Thermal Expansion | 11.0.1.29 | Kept from SO1 |
| Refined Storage | 1.12.4 | Kept from SO1 |

## Storage, food and automation (added 2026-10-18)
| Mod | Version | Why |
|---|---|---|
| Applied Energistics 2 | 15.4.11 | Alongside Refined Storage. Ex Deorum already sieves certus quartz; Inscriber Presses (meteorite-only) get a one-time recipe needing a Resonant Circuit (`kubejs/server_scripts/skyblock_fixes.js`) |
| Storage Drawers | 12.15.1 | Bulk storage for sieve output |
| Sophisticated Backpacks | 3.26.12 | Backpacks with upgrades |
| Farmer's Delight | 1.3.4 | Food. Its crops only grow wild, so cabbage, tomato, onion and rice come from sieving dirt |
| Industrial Foregoing | 3.5.22 | Skyblock automation: mob farms, plant gatherers, ore laser drill (a late ore source; watch it against sieving) |
| Mystical Agradditions | 7.0.12 | Mystical Agriculture's top tiers (needs Nether stars and End access) |

## Multiplayer
| Mod | Version | Why |
|---|---|---|
| FTB Chunks | 2001.3.8 | Claim and protect island chunks; claims follow FTB Teams parties, which the island-team bridge keeps in sync |
| FTB Ultimine | 2001.1.8 | Mine connected blocks at once (clearing grown islands) |

## Magic
| Mod | Version | Why |
|---|---|---|
| Botania | 1.20.1-456 | Kept from SO1. Its mana is the Void Condenser's first (and only, at launch) magic input |
| Ars Nouveau | 4.12.7 | Kept from SO1. Uses Source, not mana; Condenser support deferred until after release |
| Occultism | 1.158.0 | Kept from SO1 |
| Mystical Agriculture | 7.0.24 | Kept from SO1. Watch overlap with the Condenser's output rate |

## Dimensions
| Mod | Version | Why |
|---|---|---|
| Twilight Forest | 4.3.2508 | Kept from SO1. Include in the Island Cores spawn-test world |
| Blue Skies | 1.3.31 | Kept from SO1. Include in the Island Cores spawn-test world |

## Progression & scripting
| Mod | Version | Why |
|---|---|---|
| FTB Quests | 2001.4.22 | Quest book; the island score will be built on quest completion |
| FTB Teams | 2001.3.2 | Team-shared quest progress |
| KubeJS | 2001.6.5-build.26 | Bridge materials' cross-mod recipes and gates live here, not in Java |

## QoL & performance
| Mod | Version | Why |
|---|---|---|
| JEI | 15.62.0.219 | Recipe viewer (Condenser JEI category later) |
| Jade | 11.13.3 | Block/entity tooltips |
| Embeddium | 0.3.31 | Maintained 1.20.1 successor to Rubidium, which SO1 used |
| Oculus | 1.8.0 | Shaders, as in SO1 |
| FerriteCore | 6.0.1 | Memory optimisation |
| ModernFix | 5.27.85 | Load-time and memory optimisation |
| Fast Leaf Decay | 32 | Leaves drop saplings straight away: the SkyFactory start has one tree |
| AppleSkin | 2.5.1 | Food and saturation values |
| Mouse Tweaks | 2.25.1 | Inventory dragging and scrolling (client only) |
| Inventory Sorter | 23.0.11 | Middle-click sorting |
| Controlling | 12.0.2 | Searchable key binds (client only) |
| Clumps | 12.0.0.4 | Merges XP orbs (mob farms) |
| Entity Culling | 1.11.3 | Skips rendering hidden entities (client only). Distribution disabled: the CurseForge app installs it, `tools/download_mods.py` can't |

## Libraries (pulled in as required dependencies)
| Library | Version | Needed by |
|---|---|---|
| LibX | 5.0.14 | Skyblock Builder |
| CoFH Core | 11.0.2.56 | Thermal |
| Curios | 5.14.1 | Botania |
| Patchouli | 1.20.1-85 | Botania |
| GeckoLib | 4.8.4 | Occultism |
| SmartBrainLib | 1.15 | Occultism |
| Modonomicon | 1.79.3 | Occultism |
| Cucumber | 7.0.16 | Mystical Agriculture |
| Structure Gel API | 2.16.2 | Blue Skies |
| FTB Library | 2001.2.13 | FTB Quests, FTB Teams |
| Architectury API | 9.2.14 | FTB mods |
| Rhino | 2001.2.3-build.10 | KubeJS |
| mezz_config | 0.6.8 | JEI |
| GuideME | 20.1.15 | Applied Energistics 2 |
| Sophisticated Core | 1.5.12 | Sophisticated Backpacks |
| Titanium | 3.8.35 | Industrial Foregoing |
| Searchables | 1.0.3 | Controlling |

## Deliberately left out
| Mod / feature | Verdict |
|---|---|
| Ex Nihilo: Sequentia | 1.20.1 builds are NeoForge-only; replaced by Ex Deorum |
| ImmediatelyFast | Its newest file tagged 1.20.1 is a 1.20.4 build; Embeddium and ModernFix cover most of the gain |
| Origins (the mod) | Void Origins is framed as "starts", not races — avoid the comparison |
| Lightman's Currency | Sky Market dropped; add via config only if players ask |
| Enhanced Celestials / meteor mods | Sky Events trimmed to our own non-destructive meteors, after release |
| Custom Ascension / NG+ | Not built; Prestige points later instead |

## Open checks
- [x] Do Skyblock Builder teams and FTB Teams stay in sync? No: Skyblock Builder 5.1.33 has no FTB Teams compat (checked its jar, 2026-10-15), so a bridge is needed (see TODO.md)
- [x] Server side (2026-10-16, rerun 2026-10-18 with 54 jars): all server-side jars + skyorigins2core start together on a Forge 47.4.10 server with a Skyblock world, 0 KubeJS errors, 0 failed recipes, all quests, island and start templates loading. Only error: CoFH Core's client-only mixin skipped on the server (harmless). Rerun with `python tools/full_pack_test.py`
- [ ] Client side: launch the full list (with Embeddium and Oculus) in a CurseForge profile and confirm the game reaches the title screen and loads a world
- [x] Confirm Ex Deorum's sieve drops cover what Create/Mekanism/Thermal need (2026-10-15): every metal is covered once its mod adds the ore tag (osmium, uranium, tin, lead, silver, nickel, zinc); missing were fluorite, Thermal's apatite/cinnabar/niter/sulfur and Mystical Agriculture's inferium/prosperity, now added, and low metal rates raised (`tools/make_sieve_rates.py` -> `kubejs/server_scripts/sieve_rates.js`)
