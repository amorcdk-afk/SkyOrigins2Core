package com.skyorigins2core.mana;

import com.skyorigins2core.api.mana.ManaAttributes;
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
        BASE_MAX_MANA = common.comment("Every player's base maximum mana (max_mana modifiers add to it; the total never goes above " + ManaAttributes.MANA_CAP + ")")
                .defineInRange("baseMaxMana", ManaAttributes.MANA_CAP, 0, ManaAttributes.MANA_CAP);
        BASE_MANA_REGEN = common.comment("Every player's base mana regeneration per second")
                .defineInRange("baseManaRegen", 2.0, 0.0, 100_000.0);
        common.pop();
        COMMON_SPEC = common.build();

        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("hud");
        SHOW_BAR_WHEN_FULL = client.comment("Show the mana bar (right of the hotbar) when mana is full. Off hides it whenever mana is full")
                .define("showManaBarWhenFull", true);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private ManaConfig() {}
}
