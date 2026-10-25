// Things other mods only get from world generation that a void world never has.
//   Applied Energistics 2: Inscriber Presses only come from meteorites. Each press gets a one-time
//   crafting recipe that needs a Resonant Circuit (so AE2 sits behind the Arcane-Tech Bridge); after
//   that, the Inscriber copies presses as normal.
//   The End: a void world has no stronghold, so End Portal Frames are craftable. They cost Arcane Alloy,
//   obsidian and ender pearls, making the End a late goal. Skyblock Builder still generates the End's
//   main island (dragon fight) and Ex Deorum lets End Cities generate in the void.
// (Farmer's Delight crops come from the sieve: see tools/make_sieve_rates.py.)

// Press -> the item in its centre
const AE2_PRESSES = {
    calculation_processor_press: '#forge:gems/certus_quartz',
    logic_processor_press: '#forge:ingots/gold',
    engineering_processor_press: '#forge:gems/diamond',
    silicon_press: '#forge:silicon',
}

ServerEvents.recipes(event => {
    // 12 frames make a portal: 60 obsidian, 36 ender pearls and 12 Arcane Alloy, plus 12 Eyes of Ender
    event.shaped('minecraft:end_portal_frame', [
        'OPO',
        'PAP',
        'OOO',
    ], {
        O: 'minecraft:obsidian',
        P: 'minecraft:ender_pearl',
        A: '#forge:ingots/arcane_alloy',
    }).id('skyorigins2core:end_portal_frame')

    if (Platform.isLoaded('ae2')) {
        Object.keys(AE2_PRESSES).forEach(press => {
            event.shaped(`ae2:${press}`, [
                'III',
                'IXI',
                'ICI',
            ], {
                I: '#forge:ingots/iron',
                X: AE2_PRESSES[press],
                C: 'skyorigins2core:resonant_circuit',
            }).id(`skyorigins2core:ae2/${press}`)
        })
    }
})
