// Arcane-Tech Bridge, tier 1: the three materials chain magic and tech together.
//   Void Dust        <- sieving Ex Deorum dust (skyblock core loop)
//   Infused Alloy    <- Void Dust in a Botania mana pool (magic)
//   Resonant Circuit <- Infused Alloy in a Mekanism Metallurgic Infuser with redstone (tech)
// Formats copied from the mods' own 1.20.1 recipes. Tune numbers here; /reload applies changes.

ServerEvents.recipes(event => {
    // Void Dust: better meshes give better odds, like Ex Deorum's own drops
    const voidDustChance = {
        string: 0.05,
        flint: 0.08,
        iron: 0.12,
        golden: 0.16,
        diamond: 0.22,
        netherite: 0.30,
    }
    Object.keys(voidDustChance).forEach(mesh => {
        event.custom({
            type: 'exdeorum:sieve',
            ingredient: { item: 'exdeorum:dust' },
            mesh: `exdeorum:${mesh}_mesh`,
            result: 'skyorigins2core:void_dust',
            result_amount: { type: 'minecraft:binomial', n: 1.0, p: voidDustChance[mesh] },
        }).id(`skyorigins2core:sieve/dust/${mesh}/void_dust`)
    })

    // Infused Alloy: mana pool infusion
    event.custom({
        type: 'botania:mana_infusion',
        input: { tag: 'forge:dusts/void' },
        mana: 3000,
        output: { item: 'skyorigins2core:infused_alloy' },
    }).id('skyorigins2core:mana_infusion/infused_alloy')

    // Resonant Circuit: Metallurgic Infuser with redstone
    event.custom({
        type: 'mekanism:metallurgic_infusing',
        chemicalInput: { amount: 20, tag: 'mekanism:redstone' },
        itemInput: { ingredient: { tag: 'forge:ingots/infused_alloy' } },
        output: { item: 'skyorigins2core:resonant_circuit' },
    }).id('skyorigins2core:metallurgic_infusing/resonant_circuit')
})
