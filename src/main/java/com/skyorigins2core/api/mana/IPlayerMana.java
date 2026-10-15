package com.skyorigins2core.api.mana;

// Public API: a player's personal mana pool. Get it with ManaApi.get(player).
// Change mana on the server only; the player's client is kept in sync automatically.
public interface IPlayerMana
{
    // Current mana, rounded down
    int getMana();

    // The player's maximum, from the skyorigins2core:max_mana attribute
    int getMaxMana();

    // Spends `amount` if the player has that much. Returns whether they had enough; simulate = check only.
    boolean consume(int amount, boolean simulate);

    // Adds (or with a negative amount removes) mana, clamped between 0 and the maximum
    void add(int amount);

    // Sets mana, clamped between 0 and the maximum
    void set(int amount);
}
