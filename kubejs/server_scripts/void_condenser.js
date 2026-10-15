// Void Condenser: the Arcane-Tech Bridge's signature block. Needs both sides' materials and both
// sides' power (Forge Energy + Botania mana), so it's a reward for having bridged them.

ServerEvents.recipes(event => {
    event.shaped('skyorigins2core:void_condenser', [
        'APA',
        'CXC',
        'APA',
    ], {
        A: '#forge:ingots/arcane_alloy',
        P: 'botania:mana_pearl',
        C: 'skyorigins2core:resonant_circuit',
        X: 'mekanism:steel_casing',
    }).id('skyorigins2core:void_condenser')
})
