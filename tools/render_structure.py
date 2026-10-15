"""Isometric preview of structure NBT files (island templates), to check shapes without the game.

Usage: python tools/render_structure.py <out.png> <structure.nbt> [more.nbt ...]
Blocks are drawn as flat-coloured cubes; several structures are placed side by side.
"""

import gzip
import io
import struct
import sys
import zlib

TILE = 6  # half-width of a cube's top face in pixels

COLOURS = {
    "grass_block": (95, 159, 53), "dirt": (134, 96, 67), "stone": (125, 125, 125), "cobblestone": (110, 110, 110),
    "andesite": (136, 136, 137), "mossy_cobblestone": (90, 120, 80), "oak_log": (109, 85, 50),
    "oak_leaves": (60, 120, 40), "grass": (110, 170, 60), "dandelion": (240, 220, 40), "poppy": (200, 30, 30),
    "cornflower": (70, 100, 220), "azure_bluet": (220, 230, 240), "sand": (219, 207, 163),
    "sandstone": (216, 203, 155), "smooth_sandstone": (223, 214, 170), "terracotta": (152, 94, 67),
    "cut_sandstone": (217, 206, 159), "chiseled_sandstone": (210, 195, 145), "cactus": (85, 127, 43),
    "dead_bush": (148, 110, 60), "crimson_nylium": (130, 31, 31), "netherrack": (97, 38, 38),
    "magma_block": (200, 90, 20), "basalt": (80, 80, 85), "blackstone": (42, 36, 41),
    "crimson_stem": (92, 25, 29), "nether_wart_block": (115, 2, 2), "shroomlight": (240, 146, 70),
    "crimson_roots": (126, 8, 41), "crimson_fungus": (160, 30, 30), "water": (50, 90, 220),
    "prismarine": (99, 156, 151), "dark_prismarine": (51, 91, 75), "prismarine_bricks": (99, 171, 158),
    "seagrass": (40, 140, 80), "tube_coral_fan": (50, 90, 210), "brain_coral": (200, 90, 160),
    "sea_pickle": (100, 120, 40), "sea_lantern": (210, 230, 220), "chest": (160, 110, 40),
    "oak_fence": (160, 130, 80), "apothecary_default": (120, 120, 120),
}


def read_nbt(path):
    f = io.BytesIO(gzip.open(path).read())

    def payload(t):
        if t == 1: return struct.unpack(">b", f.read(1))[0]
        if t == 2: return struct.unpack(">h", f.read(2))[0]
        if t == 3: return struct.unpack(">i", f.read(4))[0]
        if t == 4: return struct.unpack(">q", f.read(8))[0]
        if t == 5: return struct.unpack(">f", f.read(4))[0]
        if t == 6: return struct.unpack(">d", f.read(8))[0]
        if t == 8: return f.read(struct.unpack(">H", f.read(2))[0]).decode()
        if t == 9:
            et = f.read(1)[0]
            return [payload(et) for _ in range(struct.unpack(">i", f.read(4))[0])]
        if t == 10:
            d = {}
            while (t2 := f.read(1)[0]) != 0:
                key = f.read(struct.unpack(">H", f.read(2))[0]).decode()
                d[key] = payload(t2)
            return d
        if t == 11:
            n = struct.unpack(">i", f.read(4))[0]
            return list(struct.unpack(f">{n}i", f.read(4 * n)))
        if t == 7: return f.read(struct.unpack(">i", f.read(4))[0])
        raise ValueError(t)

    f.read(1); f.read(struct.unpack(">H", f.read(2))[0])
    return payload(10)


def write_png(path, w, h, pixels):
    raw = b"".join(b"\x00" + bytes(pixels[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    with open(path, "wb") as out:
        out.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                  + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def cube_masks():
    """Pixel offsets of a cube's top, left and right faces, relative to its top corner."""
    t = TILE
    top, left, right = [], [], []
    for py in range(0, 4 * t + 1):
        for px in range(-2 * t, 2 * t):
            if abs(px + 0.5) / 2 + abs(py - t) <= t:
                top.append((px, py))
            elif px < 0 and t + (px + 2 * t) / 2 <= py <= 3 * t + (px + 2 * t) / 2:
                left.append((px, py))
            elif px >= 0 and 2 * t - px / 2 <= py <= 4 * t - px / 2:
                right.append((px, py))
    return top, left, right


def main():
    out, paths = sys.argv[1], sys.argv[2:]
    structures = [read_nbt(p) for p in paths]
    gap = 4
    total_x = sum(s["size"][0] + gap for s in structures)
    max_z = max(s["size"][2] for s in structures)
    max_y = max(s["size"][1] for s in structures)
    w = (total_x + max_z) * 2 * TILE + 8 * TILE
    h = (total_x + max_z) * TILE + max_y * 2 * TILE + 8 * TILE
    pixels = bytearray(w * h * 4)
    top, left, right = cube_masks()
    cubes = []
    ox = 0
    for s in structures:
        palette = [p["Name"].split(":")[1] for p in s["palette"]]
        for b in s["blocks"]:
            name = palette[b["state"]]
            if name == "air":
                continue
            x, y, z = b["pos"]
            cubes.append((x + ox, y, z, COLOURS.get(name, (255, 0, 255))))
        ox += s["size"][0] + gap
    cubes.sort(key=lambda c: (c[0] + c[2], c[1]))
    for x, y, z, colour in cubes:
        cx = (x - z) * 2 * TILE + (max_z + 2) * 2 * TILE
        cy = (x + z) * TILE - y * 2 * TILE + max_y * 2 * TILE + 2 * TILE
        for mask, shade in ((top, 1.0), (left, 0.75), (right, 0.6)):
            c = tuple(min(255, int(v * shade)) for v in colour)
            for px, py in mask:
                X, Y = cx + px, cy + py
                if 0 <= X < w and 0 <= Y < h:
                    i = (Y * w + X) * 4
                    pixels[i:i + 4] = bytes((*c, 255))
    write_png(out, w, h, pixels)
    print(f"wrote {out} ({w}x{h})")


if __name__ == "__main__":
    main()
