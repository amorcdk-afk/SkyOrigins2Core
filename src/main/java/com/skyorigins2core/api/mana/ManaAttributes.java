package com.skyorigins2core.api.mana;

import com.skyorigins2core.SkyOrigins2Core;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Public API: player attributes that control mana. Other mods raise them with ordinary attribute
// modifiers (armor, curios, potion effects), or players with /attribute. The base values come from
// the common config (skyorigins2core-common.toml) and are applied when a player joins or respawns.
public final class ManaAttributes
{
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(ForgeRegistries.ATTRIBUTES, SkyOrigins2Core.MODID);

    // Hard ceiling for maximum mana: no base value or modifier can take it higher
    public static final int MANA_CAP = 100;

    // Maximum mana, capped at MANA_CAP
    public static final RegistryObject<Attribute> MAX_MANA = ATTRIBUTES.register("max_mana",
            () -> new RangedAttribute("attribute.name.skyorigins2core.max_mana", MANA_CAP, 0.0, MANA_CAP).setSyncable(true));
    // Mana regenerated per second
    public static final RegistryObject<Attribute> MANA_REGEN = ATTRIBUTES.register("mana_regen",
            () -> new RangedAttribute("attribute.name.skyorigins2core.mana_regen", 2.0, 0.0, 100_000.0).setSyncable(true));

    private ManaAttributes() {}
}
