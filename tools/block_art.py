"""Pixel art for SkyOrigins 2 Core block textures. Writes 16x16 textures, plus an 8x preview sheet when given a preview path.

Usage: python tools/block_art.py src/main/resources/assets/skyorigins2core/textures/block [preview.png]
"""
import os
import struct
import sys
import zlib

CASING = {
    "k": (18, 18, 24), "d": (38, 38, 46), "g": (58, 58, 68), "h": (86, 86, 98), "l": (118, 118, 132),
    "v": (44, 16, 76), "p": (104, 46, 176), "q": (168, 108, 240), "w": (238, 218, 255), "b": (140, 140, 150),
}


def noise_grid(seed, chars, weights, top=None):
    """A 16x16 grid of characters picked at random (but repeatably) by weight, like vanilla's speckled
    dirt and stone. `top` optionally overrides the first rows, e.g. the green fringe on a grass side."""
    import random
    rng = random.Random(seed)
    rows = ["".join(rng.choices(chars, weights)[0] for _ in range(16)) for _ in range(16)]
    if top:
        rows[:len(top)] = top
    return rows


ISLAND = {
    "k": (40, 26, 18), "e": (78, 54, 36), "f": (104, 74, 50), "j": (128, 94, 64),        # dirt
    "s": (88, 88, 92), "t": (112, 112, 118), "u": (136, 136, 142), "x": (66, 66, 72),     # stone
    "G": (58, 112, 40), "H": (76, 140, 52), "I": (98, 166, 66), "J": (124, 190, 82),      # grass
    "v": (44, 16, 76), "p": (104, 46, 176), "q": (168, 108, 240), "w": (238, 218, 255),   # void crystal
}

TEXTURES = {
    # Floating island pieces for the Void Island Core model
    "island_grass_top": (ISLAND, noise_grid(11, "GHIJ", [3, 5, 4, 1])),
    "island_grass_side": (ISLAND, noise_grid(12, "kefj", [1, 4, 4, 2], top=[
        "HIHJHIHHIJHIHHIH",
        "GHIHGHHIHGHHIHGH",
        "GHfGHHeGHfHGeHHG",
        "eGeefGjeGefeGeje",
    ])),
    "island_dirt": (ISLAND, noise_grid(13, "kefj", [1, 4, 4, 2])),
    "island_stone": (ISLAND, noise_grid(14, "xstu", [2, 6, 4, 1])),
    # Faceted void crystal (rendered full-bright)
    "void_crystal": (ISLAND, [
        "vvvvvvpqqpvvvvvv",
        "vvvvvpqwwqpvvvvv",
        "vvvvpqqwwqqpvvvv",
        "vvvpqqwwqqqqpvvv",
        "vvpqqwwqqpqqqpvv",
        "vpqqwwqqpvpqqqpv",
        "pqqwwqqpvvvpqqqp",
        "pqwwqqpvvvvvpqqp",
        "pqwqqpvvvvvvvpqp",
        "pqqqpvvvvvvvvpqp",
        "vpqpvvvvvvvvpqpv",
        "vvppvvvvvvvvppvv",
        "vvvpvvvvvvvvpvvv",
        "vvvvpvvvvvvpvvvv",
        "vvvvvpvvvvpvvvvv",
        "vvvvvvppppvvvvvv",
    ]),
    # Plain machine casing for the frame: base, cap and pillars
    "void_condenser_casing": (CASING, [
        "kkkkkkkkkkkkkkkk",
        "khhhhhhhhhhhhhgk",
        "khbgggggggggggdk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khggggggdgggggdk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggddk",
        "khgggggggggggbdk",
        "kgdddddddddddddk",
        "kkkkkkkkkkkkkkkk",
    ]),
    # Two-armed void spiral for the floating orb (rendered full-bright)
    "void_condenser_core": (CASING, [
        "vvvvvvvvvvvvvvvv",
        "vvvvvvvvvvvvvvvv",
        "vvvvvvvvvppppvvv",
        "vvvvvvvppppppppv",
        "vvvvvvppqqqqpppp",
        "vvvvvvpqqqqqpppp",
        "vvvvvvqwwppppppp",
        "ppvvvvwwwwvvpppp",
        "ppppvvwwwwvvvvpp",
        "pppppppwwqvvvvvv",
        "ppppqqqqqpvvvvvv",
        "ppppqqqqppvvvvvv",
        "vppppppppvvvvvvv",
        "vvvppppvvvvvvvvv",
        "vvvvvvvvvvvvvvvv",
        "vvvvvvvvvvvvvvvv",
    ]),
    # Round vent glowing from the condensing void inside
    "void_condenser_top": (CASING, [
        "kkkkkkkkkkkkkkkk",
        "khhhhhhhhhhhhhgk",
        "khbggggggggggbdk",
        "khgggkkkkkkgggdk",
        "khggkdvvvvdkggdk",
        "khgkdvppppvdkgdk",
        "khgkvpqqqqpvkgdk",
        "khgkvpqwwqpvkgdk",
        "khgkvpqwwqpvkgdk",
        "khgkvpqqqqpvkgdk",
        "khgkdvppppvdkgdk",
        "khggkdvvvvdkggdk",
        "khgggkkkkkkgggdk",
        "khbggggggggggbdk",
        "kgdddddddddddddk",
        "kkkkkkkkkkkkkkkk",
    ]),
    # Plain bolted underside
    "void_condenser_bottom": (CASING, [
        "kkkkkkkkkkkkkkkk",
        "kgggggggggggggdk",
        "kgbgggggggggbgdk",
        "kggggggggggggddk",
        "kgggdddddddgggdk",
        "kgggdggggggdggdk",
        "kgggdggggggdggdk",
        "kgggdgglhggdggdk",
        "kgggdgghlggdggdk",
        "kgggdggggggdggdk",
        "kgggdggggggdggdk",
        "kgggdddddddgggdk",
        "kggggggggggggddk",
        "kgbgggggggggbddk",
        "kgdddddddddddddk",
        "kkkkkkkkkkkkkkkk",
    ]),
}


def encode(pixels, size):
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", size[0], size[1], 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def pixels_of(name):
    palette, grid = TEXTURES[name]
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), (name, [len(r) for r in grid])
    return [[(0, 0, 0, 0) if ch == "." else (*palette[ch], 255) for ch in row] for row in grid]


out_dir = sys.argv[1]
for name in TEXTURES:
    with open(os.path.join(out_dir, name + ".png"), "wb") as f:
        f.write(encode(pixels_of(name), (16, 16)))
    print("wrote", name)

if len(sys.argv) > 2:
    scale, gap = 8, 8
    width = len(TEXTURES) * 16 * scale + (len(TEXTURES) + 1) * gap
    height = 16 * scale + 2 * gap
    sheet = [[(139, 139, 139, 255)] * width for _ in range(height)]
    for i, name in enumerate(TEXTURES):
        px = pixels_of(name)
        ox = gap + i * (16 * scale + gap)
        for y in range(16 * scale):
            for x in range(16 * scale):
                sheet[gap + y][ox + x] = px[y // scale][x // scale]
    with open(sys.argv[2], "wb") as f:
        f.write(encode(sheet, (width, height)))
    print("preview", sys.argv[2])
