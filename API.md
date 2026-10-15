# SkyOrigins 2 Core API

Two systems for your other Forge 1.20.1 mods to build on: **player mana** and **Void Charge** energy.
Use only the classes in `com.skyorigins2core.api`; everything else in the mod can change between versions.

| System | What it is | Where |
|---|---|---|
| Player mana | Every player has a mana pool that regenerates, shown as a blue bar above the XP bar | `api.mana` |
| Void Charge (VC) | An energy unit (1 VC = 4 FE) with a capability and a ready-made storage class. This mod adds no machines; yours do | `api.energy` |

## 1. Depend on SkyOrigins 2 Core

**Publish this mod** (in this project, after any change to the API):

```
gradlew publish
```

That writes `com.skyorigins2core:skyorigins2core:<mod_version>` to the `mcmodsrepo/` folder in this project.

**In your other mod's `build.gradle`:**

```groovy
repositories {
    maven { url "file:///C:/Users/Allan/Documents/skyorigins2/skyorigins 2 core/mcmodsrepo" }
}

dependencies {
    implementation fg.deobf("com.skyorigins2core:skyorigins2core:1.0.0")
}
```

**In your other mod's `src/main/resources/META-INF/mods.toml`:**

```toml
[[dependencies.yourmodid]]
    modId="skyorigins2core"
    mandatory=true
    versionRange="[1.0.0,)"
    ordering="AFTER"
    side="BOTH"
```

## 2. Player mana (`com.skyorigins2core.api.mana`)

Change mana **on the server**; the player's client and mana bar update automatically.

```java
import com.skyorigins2core.api.mana.ManaApi;

// In an item's use() method, server side:
if (!level.isClientSide && ManaApi.tryConsume(player, 25)) {
    // the spell happens; 25 mana was spent
}

ManaApi.has(player, 25);       // can they afford it? (spends nothing)
ManaApi.getMana(player);       // current mana
ManaApi.add(player, 10);       // give mana (negative to drain), clamped to 0..max
ManaApi.get(player)            // Optional<IPlayerMana> for everything else, e.g. getMaxMana()
```

**More mana from gear or effects:** use the attributes, not code. Any attribute modifier works: armor, Curios, potion effects, or `/attribute`.

```java
import com.skyorigins2core.api.mana.ManaAttributes;

// e.g. in an ArmorItem's getDefaultAttributeModifiers:
builder.put(ManaAttributes.MAX_MANA.get(),
        new AttributeModifier(UUID.fromString("<a fixed uuid>"), "Mana helmet", 25, AttributeModifier.Operation.ADDITION));  // only helps if base max is below 100
builder.put(ManaAttributes.MANA_REGEN.get(),
        new AttributeModifier(UUID.fromString("<another fixed uuid>"), "Mana helmet regen", 1.0, AttributeModifier.Operation.ADDITION));
```

| Attribute | Meaning | Base value |
|---|---|---|
| `skyorigins2core:max_mana` | Maximum mana, **capped at 100** (`ManaAttributes.MANA_CAP`) | 100 |
| `skyorigins2core:mana_regen` | Mana regenerated per second | 2 |

Base values are set in `config/skyorigins2core-common.toml` and applied when a player joins or respawns.
Max mana can never go above 100. With the default base of 100, `max_mana` boosts have no effect, so lower
`baseMaxMana` (for example to 50) if you want gear to raise it; `mana_regen` boosts always work.

Mana is saved with the player, refills on respawn after death, and carries through the End portal.

Testing commands (need cheats): `/skyorigins mana get|set|add <players> [amount]`.

## 3. Void Charge (`com.skyorigins2core.api.energy`)

Works like Forge Energy: a block entity exposes the `SkyOriginsCapabilities.VOID_CHARGE` capability, and neighbours push charge into it or pull it out.

**A machine that uses Void Charge:**

```java
import com.skyorigins2core.api.SkyOriginsCapabilities;
import com.skyorigins2core.api.energy.IVoidChargeStorage;
import com.skyorigins2core.api.energy.VoidCharge;
import com.skyorigins2core.api.energy.VoidChargeStorage;

public class MyMachineBlockEntity extends BlockEntity {
    // capacity 10,000 VC; neighbours may push in up to 100 VC per call, and pull none out
    private final VoidChargeStorage charge = new VoidChargeStorage(10_000, 100, 0) {
        @Override protected void onChanged() { setChanged(); }
    };
    private final LazyOptional<IVoidChargeStorage> chargeCap = LazyOptional.of(() -> charge);

    void tick() {
        if (charge.consume(20)) {
            // the machine works this tick, using 20 VC
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return cap == SkyOriginsCapabilities.VOID_CHARGE ? chargeCap.cast() : super.getCapability(cap, side);
    }

    @Override public void invalidateCaps() { super.invalidateCaps(); chargeCap.invalidate(); }

    // save/load: tag.put("Charge", charge.serializeNBT());  charge.deserializeNBT(tag.get("Charge"));
}
```

**A generator that pushes charge out:**

```java
charge.generate(20);                                   // add charge, ignoring the push-in limit
for (Direction side : Direction.values())
    VoidCharge.pushTo(charge, level, worldPosition, side, 200);   // give neighbours up to 200 VC each
```

**Helpers:**

| Helper | Use |
|---|---|
| `VoidChargeStorage` | Ready-made storage: `receiveCharge` / `extractCharge` (limited per call), plus `generate` / `consume` for the owning machine |
| `VoidCharge.pushTo(...)` | Move charge into the block on one side |
| `VoidCharge.at(level, pos, side)` | The Void Charge storage on a block's side, if any |
| `VoidCharge.receiveOnly(storage)` / `extractOnly(storage)` | Views for input-only or output-only faces |
| `VoidCharge.FE_PER_VC` | 4: Forge Energy per VC, for converters |
