// Botanist's Grove start: the island has no stone, gravel or lava, so its first resources come
// from Botania instead of sieves. These recipes exist for every player (KubeJS recipes are global),
// so each one is priced to be slower or dearer than the sieve route and gives the SkyFactory start
// no shortcut. No existing recipe is removed or changed.
//   Pure Daisy:     dirt -> stone (twice the normal Pure Daisy time) -> livingrock -> Mana Pool
//   Mana infusion:  stone -> copper ore chunk (1,000 mana), cobblestone -> iron ore chunk (1,500 mana)
// For comparison, a String Mesh finds a copper chunk in ~1 of 12 gravel and an iron chunk in ~1 of 10.
// 4 chunks craft an ore block (Ex Deorum), so one ingot costs 4,000 / 6,000 mana; Botania turns an
// iron ingot into manasteel for 3,000. Tune here after playtesting; /reload applies changes.

ServerEvents.recipes(event => {
    event.custom({
        type: 'botania:pure_daisy',
        input: { type: 'block', block: 'minecraft:dirt' },
        output: { name: 'minecraft:stone' },
        time: 300,
    }).id('skyorigins2core:pure_daisy/dirt_to_stone')

    event.custom({
        type: 'botania:mana_infusion',
        input: { item: 'minecraft:stone' },
        mana: 1000,
        output: { item: 'exdeorum:copper_ore_chunk' },
    }).id('skyorigins2core:mana_infusion/copper_ore_chunk')

    event.custom({
        type: 'botania:mana_infusion',
        input: { item: 'minecraft:cobblestone' },
        mana: 1500,
        output: { item: 'exdeorum:iron_ore_chunk' },
    }).id('skyorigins2core:mana_infusion/iron_ore_chunk')
})
