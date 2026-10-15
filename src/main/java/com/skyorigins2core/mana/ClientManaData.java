package com.skyorigins2core.mana;

// The local player's mana as last sent by the server, read by the mana bar.
// No client-only imports, so the sync packet can safely refer to it on either side.
public final class ClientManaData
{
    private static int mana;
    private static int maxMana;

    private ClientManaData() {}

    public static void set(int mana, int maxMana)
    {
        ClientManaData.mana = mana;
        ClientManaData.maxMana = maxMana;
    }

    public static int mana() { return mana; }
    public static int maxMana() { return maxMana; }
}
