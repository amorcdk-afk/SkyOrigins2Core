// Island Core recipes. The verdict: cores cost processed materials, so a new island is a mid-game
// reward for using the Arcane-Tech Bridge rather than something you craft from your starting tree.

ServerEvents.recipes(event => {
    // Void Island Core: grows a random island (void, desert or crimson crust)
    event.shaped('skyorigins2core:void_island_core', [
        'DSD',
        'SAS',
        'DSD',
    ], {
        D: '#forge:dusts/void',
        S: '#minecraft:dirt',
        A: '#forge:ingots/arcane_alloy',
    }).id('skyorigins2core:void_island_core')
})
