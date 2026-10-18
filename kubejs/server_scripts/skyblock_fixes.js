// Things other mods only get from world generation that a void world never has.
//   Applied Energistics 2: Inscriber Presses only come from meteorites. Each press gets a one-time
//   crafting recipe that needs a Resonant Circuit (so AE2 sits behind the Arcane-Tech Bridge); after
//   that, the Inscriber copies presses as normal.
// (Farmer's Delight crops come from the sieve: see tools/make_sieve_rates.py.)

// Press -> the item in its centre
const AE2_PRESSES = {
    calculation_processor_press: '#forge:gems/certus_quartz',
    logic_processor_press: '#forge:ingots/gold',
    engineering_processor_press: '#forge:gems/diamond',
    silicon_press: '#forge:silicon',
}

ServerEvents.recipes(event => {
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
