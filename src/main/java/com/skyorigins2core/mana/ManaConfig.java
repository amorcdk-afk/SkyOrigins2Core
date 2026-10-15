package com.skyorigins2core.mana;

import net.minecraftforge.common.ForgeConfigSpec;

// skyorigins2core-common.toml: base mana values (raised further by max_mana / mana_regen attribute modifiers)
// skyorigins2core-client.toml: how the mana bar is shown
public final class ManaConfig
{
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue BASE_MAX_MANA;
    public static final ForgeConfigSpec.DoubleValue BASE_MANA_REGEN;

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_BAR_WHEN_FULL;

    static
    {
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();
        common.push("mana");
        BASE_MAX_MANA = common.comment("Every player's base maximum mana")
                .defineInRange("baseMaxMana", 100, 0, 1_000_000);
        BASE_MANA_REGEN = common.comment("Every player's base mana regeneration per second")
                .defineInRange("baseManaRegen", 2.0, 0.0, 100_000.0);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("hud");
        SHOW_BAR_WHEN_FULL = client.comment("Show the mana bar when mana is full. Off hides it at full mana, which moves hearts and hunger down when it hides")
                .define("showManaBarWhenFull", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private ManaConfig() {}
}
