"""Build the Island Core island templates (structure NBT) for SkyOrigins 2.

Writes src/main/resources/data/skyorigins2core/structures/islands/*.nbt, which the island files in
data/skyorigins2core/islands/*.json point at with "template". Each island is shaped here in code
(seeded, so the output is the same every run) and can be tweaked and rebuilt:

    python tools/make_islands.py

Rules the templates follow, because cores grow them bottom-up one block at a time:
  - every column is solid from its bottom to its top, and sand or gravel at the bottom of a
    column becomes sandstone, so nothing can fall (a falling rim would drain the reef's pool)
  - the centre column's top block is where the core sits; it's saved as "anchor" in the NBT, and
    the core's spot becomes that block when the island finishes
  - leaves are persistent, since a canopy can be placed before every log under it exists
The radius and depth printed for each island go in its JSON (biome stamp and mob spawning use them).
"""

import math
import os
import random

from make_templates import Int, Structure

OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                       "data", "skyorigins2core", "structures", "islands")


class Noise:
    """Smooth 2D value noise, seeded."""

    def __init__(self, seed, scale):
        self.rng = random.Random(seed)
        self.scale = scale
        self.grid = {}

    def _at(self, ix, iz):
        if (ix, iz) not in self.grid:
            self.grid[(ix, iz)] = self.rng.random() * 2 - 1
        return self.grid[(ix, iz)]

    def __call__(self, x, z):
        fx, fz = x / self.scale, z / self.scale
        ix, iz = math.floor(fx), math.floor(fz)
        tx, tz = fx - ix, fz - iz
        sx, sz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
        a = self._at(ix, iz) + (self._at(ix + 1, iz) - self._at(ix, iz)) * sx
        b = self._at(ix, iz + 1) + (self._at(ix + 1, iz + 1) - self._at(ix, iz + 1)) * sx
        return a + (b - a) * sz


class Island:
    """An organic floating island: a bumpy top over a tapering, dripping underside.

    Coordinates are centred on the core column (0, 0); top heights are relative to the core's Y.
    """

    def __init__(self, seed, radius, depth, bumps=1.0):
        self.rng = random.Random(seed)
        edge = Noise(seed + 1, 3.5)
        top = Noise(seed + 2, 4.0)
        under = Noise(seed + 3, 2.5)
        self.radius = radius
        self.columns = {}  # (x, z) -> (bottom_y, top_y)
        for x in range(-radius - 2, radius + 3):
            for z in range(-radius - 2, radius + 3):
                d = math.hypot(x, z)
                r = radius + edge(x, z) * 1.6
                if d > r:
                    continue
                t = d / r
                top_y = 0 if (x, z) == (0, 0) else round(top(x, z) * bumps * (1 - t) ** 0.5)
                bottom = -max(1, round(depth * (1 - t ** 1.5) + under(x, z) * 1.5))
                self.columns[(x, z)] = (min(bottom, top_y), top_y)
        # A few stalactites hanging below the underside
        inner = [p for p in self.columns if math.hypot(*p) < radius * 0.6]
        for p in self.rng.sample(inner, k=min(4, len(inner))):
            bottom, top_y = self.columns[p]
            self.columns[p] = (bottom - self.rng.randint(1, 3), top_y)

    def depth(self):
        return -min(b for b, _ in self.columns.values())

    def top(self, x, z):
        return self.columns[(x, z)][1]

    def fill(self, s, offset, layer_for):
        """layer_for(x, z, y, depth_below_top) -> block name (or (name, props))."""
        for (x, z), (bottom, top_y) in self.columns.items():
            for y in range(bottom, top_y + 1):
                block = layer_for(x, z, y, top_y - y)
                name, props = block if isinstance(block, tuple) else (block, None)
                s.set(x + offset[0], y + offset[1], z + offset[2], name, props)

    def surface_spots(self, margin=1, avoid_centre=2):
        """Top positions away from the edge and from the core column, for decorations."""
        spots = []
        for (x, z), (_, top_y) in self.columns.items():
            if math.hypot(x, z) < avoid_centre:
                continue
            if all((x + dx, z + dz) in self.columns for dx in range(-margin, margin + 1) for dz in range(-margin, margin + 1)):
                spots.append((x, top_y, z))
        return spots


FALLING = {"minecraft:sand", "minecraft:red_sand", "minecraft:gravel"}


def build(name, island, fill, decorate):
    s = Structure()
    r = island.radius + 3
    offset = (r, island.depth() + 1, r)  # shift so every coordinate is >= 0
    island.fill(s, offset, fill)
    decorate(s, island, offset)
    # Sand or gravel with nothing under it (the thin edge of a column) would fall: use sandstone there
    for (x, y, z), (block, props, nbt) in list(s.blocks.items()):
        if block in FALLING and s.get(x, y - 1, z) is None:
            s.set(x, y, z, "minecraft:sandstone")
    anchor = [Int(offset[0]), Int(island.top(0, 0) + offset[1]), Int(offset[2])]
    s.save_to(OUT_DIR, f"{name}.nbt", {"anchor": anchor})
    print(f"  {name}: radius {island.radius + 2}, depth {island.depth()} (put these in islands/{name}.json)")


def put(s, offset, x, y, z, name, props=None):
    s.set(x + offset[0], y + offset[1], z + offset[2], name, props)


def free_spot(rng, spots, used, spacing):
    rng.shuffle(spots)
    for spot in spots:
        if all(abs(spot[0] - u[0]) > spacing or abs(spot[2] - u[2]) > spacing for u in used):
            used.append(spot)
            return spot
    return None


def tree(s, offset, x, y, z, height=5):
    for dy in range(1, height + 1):
        put(s, offset, x, y + dy, z, "minecraft:oak_log", {"axis": "y"})
    top = y + height
    leaves = {"distance": "1", "persistent": "true", "waterlogged": "false"}
    for ly, rad in ((top - 1, 2), (top, 2), (top + 1, 1), (top + 2, 1)):
        for dx in range(-rad, rad + 1):
            for dz in range(-rad, rad + 1):
                if rad == 2 and abs(dx) == 2 and abs(dz) == 2:
                    continue
                if ly == top + 2 and abs(dx) + abs(dz) == 2:
                    continue
                if (dx, dz) == (0, 0) and ly <= top:
                    continue
                put(s, offset, x + dx, ly, z + dz, "minecraft:oak_leaves", leaves)


# --- the four islands ---------------------------------------------------------------------------

def void_island():
    island = Island(seed=101, radius=6, depth=8, bumps=1.2)
    rng = random.Random(1011)

    def fill(x, z, y, below):
        if below == 0:
            return "minecraft:grass_block", {"snowy": "false"}
        if below <= 2:
            return "minecraft:dirt"
        roll = rng.random()
        return "minecraft:andesite" if roll < 0.15 else "minecraft:cobblestone" if roll < 0.25 else "minecraft:stone"

    def decorate(s, isl, offset):
        spots = isl.surface_spots(margin=2, avoid_centre=2.5)
        used = []
        x, y, z = free_spot(rng, spots, used, 3)
        tree(s, offset, x, y, z)
        # A mossy boulder
        bx, by, bz = free_spot(rng, spots, used, 2)
        for dx, dz in ((0, 0), (1, 0), (0, 1)):
            put(s, offset, bx + dx, isl.top(bx + dx, bz + dz) + 1, bz + dz, "minecraft:mossy_cobblestone")
        put(s, offset, bx, isl.top(bx, bz) + 2, bz, "minecraft:mossy_cobblestone")
        plants = ["minecraft:grass"] * 6 + ["minecraft:dandelion", "minecraft:poppy", "minecraft:cornflower", "minecraft:azure_bluet"]
        for spot in isl.surface_spots(margin=0, avoid_centre=1.5):
            x, y, z = spot
            if s.get(x + offset[0], y + 1 + offset[1], z + offset[2]) is None and rng.random() < 0.3:
                put(s, offset, x, y + 1, z, rng.choice(plants))

    build("void_island", island, fill, decorate)


def desert_island():
    island = Island(seed=202, radius=6, depth=7, bumps=1.6)
    rng = random.Random(2021)

    def fill(x, z, y, below):
        if below <= 2:
            return "minecraft:sand"
        roll = rng.random()
        return "minecraft:smooth_sandstone" if roll < 0.2 else "minecraft:terracotta" if roll < 0.28 else "minecraft:sandstone"

    def decorate(s, isl, offset):
        used = []
        spots = [p for p in isl.surface_spots(margin=1, avoid_centre=2.5)
                 if all(isl.top(p[0] + dx, p[2] + dz) <= p[1] for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
        for _ in range(3):  # cacti, kept two blocks from anything else
            spot = free_spot(rng, spots, used, 2)
            if spot:
                x, y, z = spot
                for dy in range(1, rng.randint(2, 3) + 1):
                    put(s, offset, x, y + dy, z, "minecraft:cactus", {"age": "0"})
        # A broken sandstone pillar
        spot = free_spot(rng, spots, used, 2)
        if spot:
            x, y, z = spot
            for dy in range(1, 4):
                put(s, offset, x, y + dy, z, "minecraft:chiseled_sandstone" if dy == 2 else "minecraft:cut_sandstone")
        for spot in isl.surface_spots(margin=0, avoid_centre=1.5):
            x, y, z = spot
            if s.get(x + offset[0], y + 1 + offset[1], z + offset[2]) is None and rng.random() < 0.12:
                near = any(s.get(x + dx + offset[0], y + 1 + offset[1], z + dz + offset[2]) == "minecraft:cactus"
                           for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
                if not near:
                    put(s, offset, x, y + 1, z, "minecraft:dead_bush")

    build("desert_island", island, fill, decorate)


def crust_island():
    island = Island(seed=303, radius=6, depth=8, bumps=1.3)
    rng = random.Random(3031)

    def fill(x, z, y, below):
        if below == 0:
            return "minecraft:crimson_nylium"
        if below <= 2:
            return "minecraft:magma_block" if rng.random() < 0.06 else "minecraft:netherrack"
        roll = rng.random()
        return "minecraft:basalt" if roll < 0.15 else "minecraft:netherrack" if roll < 0.3 else "minecraft:blackstone"

    def decorate(s, isl, offset):
        used = []
        spots = isl.surface_spots(margin=2, avoid_centre=2.5)
        # A huge crimson fungus: stem with a wart cap and shroomlights
        x, y, z = free_spot(rng, spots, used, 3)
        for dy in range(1, 6):
            put(s, offset, x, y + dy, z, "minecraft:crimson_stem", {"axis": "y"})
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2:
                    continue
                put(s, offset, x + dx, y + 6, z + dz, "minecraft:nether_wart_block")
                if max(abs(dx), abs(dz)) == 2 and rng.random() < 0.5:
                    put(s, offset, x + dx, y + 5, z + dz, "minecraft:nether_wart_block")
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                put(s, offset, x + dx, y + 7, z + dz, "minecraft:nether_wart_block")
        put(s, offset, x + 1, y + 5, z, "minecraft:shroomlight")
        put(s, offset, x - 1, y + 5, z + 1, "minecraft:shroomlight")
        plants = ["minecraft:crimson_roots"] * 4 + ["minecraft:crimson_fungus"]
        for spot in isl.surface_spots(margin=0, avoid_centre=1.5):
            x, y, z = spot
            if s.get(x + offset[0], y + 1 + offset[1], z + offset[2]) is None and rng.random() < 0.25:
                put(s, offset, x, y + 1, z, rng.choice(plants))

    build("crust_island", island, fill, decorate)


def reef_island():
    island = Island(seed=404, radius=6, depth=7, bumps=0.0)  # flat top so the pool sits level
    rng = random.Random(4041)
    pool_radius, pool_depth = 3.3, 2

    def in_pool(x, z):
        return math.hypot(x, z) <= pool_radius

    def fill(x, z, y, below):
        if in_pool(x, z) and below < pool_depth:
            return "minecraft:water", {"level": "0"}
        if below <= 2:
            return "minecraft:sand"
        roll = rng.random()
        return "minecraft:dark_prismarine" if roll < 0.15 else "minecraft:prismarine_bricks" if roll < 0.25 else "minecraft:prismarine"

    def decorate(s, isl, offset):
        # Sea life on the pool floor, away from the core column (the pool's centre)
        life = [("minecraft:seagrass", None)] * 4 + [
            ("minecraft:tube_coral_fan", {"waterlogged": "true"}),
            ("minecraft:brain_coral", {"waterlogged": "true"}),
            ("minecraft:sea_pickle", {"pickles": "2", "waterlogged": "true"}),
        ]
        for (x, z) in isl.columns:
            if in_pool(x, z) and math.hypot(x, z) >= 1.5 and rng.random() < 0.45:
                name, props = rng.choice(life)
                put(s, offset, x, isl.top(x, z) - pool_depth + 1, z, name, props)
        # A sea lantern set into the underside
        put(s, offset, 0, isl.top(0, 0) - 4, 0, "minecraft:sea_lantern")

    build("reef_island", island, fill, decorate)


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    void_island()
    desert_island()
    crust_island()
    reef_island()
