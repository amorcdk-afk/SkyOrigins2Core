// Arcane-Tech Bridge gates: a few early-to-mid milestones need the other side's material,
// so players can't ignore half the pack. Keep this list short (3-5 gates) and explain each in a quest.
//   Tech needs magic:  Metallurgic Infuser, Enrichment Chamber  <- Arcane Alloy (mana pool)
//   Magic needs tech:  Runic Altar, Terrestrial Agglomeration Plate  <- Resonant Circuit (Metallurgic Infuser)
// Arcane Alloy itself is never gated (it only needs a mana pool), so the chain can't deadlock.
// Each original recipe is removed and replaced with an otherwise identical one.

ServerEvents.recipes(event => {
    const alloy = '#forge:ingots/arcane_alloy'
    const circuit = 'skyorigins2core:resonant_circuit'

    // Metallurgic Infuser: the osmium ingot becomes Arcane Alloy
    event.remove({ id: 'mekanism:metallurgic_infuser' })
    event.shaped('mekanism:metallurgic_infuser', [
        'I#I',
        'RAR',
        'I#I',
    ], {
        I: '#forge:ingots/iron',
        '#': 'minecraft:furnace',
        R: '#forge:dusts/redstone',
        A: alloy,
    }).id('skyorigins2core:gate/metallurgic_infuser')

    // Enrichment Chamber: the two iron ingots become Arcane Alloy
    event.remove({ id: 'mekanism:enrichment_chamber' })
    event.shaped('mekanism:enrichment_chamber', [
        'ACA',
        'JXJ',
        'ACA',
    ], {
        A: '#mekanism:alloys/basic',
        C: '#forge:circuits/basic',
        J: alloy,
        X: 'mekanism:steel_casing',
    }).id('skyorigins2core:gate/enrichment_chamber')

    // Runic Altar: adds a Resonant Circuit underneath
    event.remove({ id: 'botania:runic_altar' })
    event.shaped('botania:runic_altar', [
        'SSS',
        'SPS',
        ' C ',
    ], {
        S: 'botania:livingrock',
        P: 'botania:mana_pearl',
        C: circuit,
    }).id('skyorigins2core:gate/runic_altar')

    // Terrestrial Agglomeration Plate: the middle lapis block becomes a Resonant Circuit
    event.remove({ id: 'botania:terra_plate' })
    event.shaped('botania:terra_plate', [
        'LCL',
        '0M1',
        '283',
    ], {
        L: 'minecraft:lapis_block',
        C: circuit,
        M: '#botania:manasteel_blocks',
        0: 'botania:rune_water',
        1: 'botania:rune_fire',
        2: 'botania:rune_earth',
        3: 'botania:rune_air',
        8: 'botania:rune_mana',
    }).id('skyorigins2core:gate/terra_plate')
})
