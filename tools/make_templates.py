"""Build the Skyblock Builder start templates (structure NBT) for SkyOrigins 2.

Writes config/skyblockbuilder/templates/*.nbt. Islands are described in code below,
so they can be tweaked and rebuilt instead of hand-edited in game:

    python tools/make_templates.py

Each template's player spawn is set in config/skyblockbuilder/templates.json5; keep the
SPAWN values printed by this script in sync with it.
"""

import gzip
import io
import os
import struct
from collections import deque

DATA_VERSION = 3465  # Minecraft 1.20.1
OUT_DIR = os.path.join(os.path.dirname(__file__), "..", "config", "skyblockbuilder", "templates")


# --- minimal NBT writer -------------------------------------------------------------------------

class Int(int): pass
class Byte(int): pass
class IntList(list): pass


def _payload(out, value):
    if isinstance(value, Byte):
        out.write(struct.pack(">b", value))
    elif isinstance(value, (Int, int)) and not isinstance(value, bool):
        out.write(struct.pack(">i", value))
    elif isinstance(value, str):
        data = value.encode("utf-8")
        out.write(struct.pack(">H", len(data)) + data)
    elif isinstance(value, list):
        if not value:
            out.write(bytes([0]) + struct.pack(">i", 0))
        else:
            out.write(bytes([_tag(value[0])]) + struct.pack(">i", len(value)))
            for v in value:
                _payload(out, v)
    elif isinstance(value, dict):
        for k, v in value.items():
            out.write(bytes([_tag(v)]))
            _payload(out, k)
            _payload(out, v)
        out.write(bytes([0]))
    else:
        raise TypeError(value)


def _tag(value):
    if isinstance(value, Byte): return 1
    if isinstance(value, int): return 3
    if isinstance(value, str): return 8
    if isinstance(value, list): return 9
    if isinstance(value, dict): return 10
    raise TypeError(value)


def write_nbt(path, root):
    out = io.BytesIO()
    out.write(bytes([10]))
    _payload(out, "")
    _payload(out, root)
    with gzip.open(path, "wb") as f:
        f.write(out.getvalue())


# --- structure builder --------------------------------------------------------------------------

class Structure:
    def __init__(self):
        self.blocks = {}  # (x, y, z) -> (name, props, nbt)

    def set(self, x, y, z, name, props=None, nbt=None):
        self.blocks[(x, y, z)] = (name, props or {}, nbt)

    def get(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b[0] if b else None

    def save(self, filename):
        xs, ys, zs = zip(*self.blocks)
        size = [max(xs) + 1, max(ys) + 1, max(zs) + 1]
        palette, index, blocks = [], {}, []
        for pos in sorted(self.blocks, key=lambda p: (p[1], p[2], p[0])):
            name, props, nbt = self.blocks[pos]
            key = (name, tuple(sorted(props.items())))
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": name}
                if props:
                    entry["Properties"] = dict(sorted(props.items()))
                palette.append(entry)
            block = {"pos": [Int(c) for c in pos], "state": Int(index[key])}
            if nbt:
                block["nbt"] = nbt
            blocks.append(block)
        write_nbt(os.path.join(OUT_DIR, filename), {
            "DataVersion": Int(DATA_VERSION),
            "size": [Int(s) for s in size],
            "palette": palette,
            "blocks": blocks,
            "entities": [],
        })
        print(f"{filename}: size {size}, {len(blocks)} blocks")


def chest(items, facing="north"):
    nbt = {"id": "minecraft:chest", "Items": [
        {"Slot": Byte(slot), "id": item, "Count": Byte(count)} for slot, (item, count) in enumerate(items)
    ]}
    return "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": "false"}, nbt


def oak_tree(s, x, y, z, trunk=5):
    """A vanilla-shaped oak: trunk on top of (x, y-1, z), canopy around the top."""
    top = y + trunk - 1
    for dy in range(trunk):
        s.set(x, y + dy, z, "minecraft:oak_log", {"axis": "y"})
    leaves = []
    for ly in (top - 2, top - 1):  # two wide layers, corners trimmed
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 and abs(dz) == 2:
                    continue
                leaves.append((x + dx, ly, z + dz))
    for ly in (top, top + 1):  # two narrow layers
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if ly == top + 1 and abs(dx) + abs(dz) == 2:
                    continue
                leaves.append((x + dx, ly, z + dz))
    leaves = [p for p in leaves if s.get(*p) is None]
    # Leaf "distance" = steps to the nearest log, so the tree is stable when placed
    dist = {}
    queue = deque()
    for p in leaves:
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            if s.get(p[0] + d[0], p[1] + d[1], p[2] + d[2]) == "minecraft:oak_log":
                dist[p] = 1
                queue.append(p)
                break
    leaf_set = set(leaves)
    while queue:
        p = queue.popleft()
        for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            q = (p[0] + d[0], p[1] + d[1], p[2] + d[2])
            if q in leaf_set and q not in dist:
                dist[q] = dist[p] + 1
                queue.append(q)
    for p in leaves:
        s.set(*p, "minecraft:oak_leaves",
              {"distance": str(min(dist.get(p, 7), 7)), "persistent": "false", "waterlogged": "false"})


# --- the islands --------------------------------------------------------------------------------

def skyfactory():
    """Main start, SkyFactory style: one oak tree on a single grass block. Everything else
    comes from the tree (saplings, sticks, leaves -> Ex Deorum crook/compost -> dirt -> sieve)."""
    s = Structure()
    s.set(2, 0, 2, "minecraft:grass_block", {"snowy": "false"})
    oak_tree(s, 2, 1, 2)
    s.save("skyfactory.nbt")
    print("  SPAWN skyfactory: [2, 7, 2] (on top of the tree)")


def botanists_grove():
    """Optional start: a 7x7 grass island with mystical flowers, a 2-block infinite water pond,
    a petal apothecary, a fence railing so thrown items don't fall into the void, and a chest
    with an oak sapling, seeds, bone meal, a water bucket and one copper ingot (for the first Mana Spreader).
    No spreader or pool: the player builds those. Grove recipes: kubejs/server_scripts/botanists_grove.js"""
    s = Structure()
    lo, hi = 1, 7  # island spans x/z 1..7, leaving a 1-block margin
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            s.set(x, 0, z, "minecraft:dirt")
            s.set(x, 1, z, "minecraft:grass_block", {"snowy": "false"})
    # Two water sources side by side make an infinite water pond (apothecary, Hydroangeas)
    for x in (2, 3):
        s.set(x, 1, 2, "minecraft:water", {"level": "0"})
    # Railing around the edge; fences connect to each other along the ring
    ring = [(x, z) for x in range(lo, hi + 1) for z in range(lo, hi + 1) if x in (lo, hi) or z in (lo, hi)]
    ring_set = set(ring)
    for x, z in ring:
        props = {d: str(n in ring_set).lower() for d, n in
                 (("north", (x, z - 1)), ("south", (x, z + 1)), ("west", (x - 1, z)), ("east", (x + 1, z)))}
        props["waterlogged"] = "false"
        s.set(x, 2, z, "minecraft:oak_fence", props)
    # Mystical flowers: enough petals for a Pure Daisy (4 white), an Endoflame
    # (2 brown, red, light gray) and a Hydroangeas (2 blue, 2 cyan), plus a few spare colours
    flowers = [
        ("white", 4, 2), ("white", 5, 2), ("white", 6, 3),
        ("brown", 2, 5), ("brown", 2, 6), ("red", 3, 6), ("light_gray", 6, 5),
        ("blue", 5, 6), ("cyan", 6, 6), ("lime", 2, 4), ("pink", 6, 4),
    ]
    for colour, x, z in flowers:
        s.set(x, 2, z, f"botania:{colour}_mystical_flower")
    s.set(4, 2, 3, "botania:apothecary_default")
    s.set(4, 2, 6, *chest([
        ("minecraft:oak_sapling", 1),
        ("minecraft:wheat_seeds", 2),
        ("minecraft:bone_meal", 16),
        ("minecraft:water_bucket", 1),  # fills the Petal Apothecary; refill it from the pond
        ("minecraft:copper_ingot", 1),  # for the first Mana Spreader; later copper comes from mana
    ], facing="north"))
    s.save("botanists_grove.nbt")
    print("  SPAWN botanists_grove: [4, 2, 4] (island centre)")


if __name__ == "__main__":
    os.makedirs(OUT_DIR, exist_ok=True)
    skyfactory()
    botanists_grove()
