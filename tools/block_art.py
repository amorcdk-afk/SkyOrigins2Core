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

TEXTURES = {
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
