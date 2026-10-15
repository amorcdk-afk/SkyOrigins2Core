package com.skyorigins2core.api.mana;

import com.skyorigins2core.api.SkyOriginsCapabilities;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

// Public API: convenience helpers for player mana. Call on the server; clients see the result automatically.
//   if (ManaApi.tryConsume(player, 25)) castSpell();
public final class ManaApi
{
    private ManaApi() {}

    public static Optional<IPlayerMana> get(Player player)
    {
        return player.getCapability(SkyOriginsCapabilities.PLAYER_MANA).resolve();
    }

    // Spends mana if the player has enough; returns false (and spends nothing) otherwise
    public static boolean tryConsume(Player player, int amount)
    {
        return get(player).map(mana -> mana.consume(amount, false)).orElse(false);
    }

    public static boolean has(Player player, int amount)
    {
        return get(player).map(mana -> mana.consume(amount, true)).orElse(false);
    }

    public static int getMana(Player player)
    {
        return get(player).map(IPlayerMana::getMana).orElse(0);
    }

    public static void add(Player player, int amount)
    {
        get(player).ifPresent(mana -> mana.add(amount));
    }
}
