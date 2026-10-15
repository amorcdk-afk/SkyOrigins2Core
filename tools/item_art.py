"""Pixel art for the Bridge items. Writes 16x16 textures, plus an 8x preview sheet when given a preview path."""
import os
import struct
import sys
import zlib

TEXTURES = {
    # A heap of glittering void dust with two small clumps, like vanilla glowstone/redstone dust
    "void_dust": ({
        "o": (20, 6, 34), "a": (50, 18, 82), "b": (86, 36, 132), "c": (134, 72, 196),
        "d": (190, 140, 250), "w": (245, 232, 255), "s": (120, 220, 255),
    }, [
        "................",
        "..........w.....",
        "..ooo...........",
        ".obcdo.....ooo..",
        ".obbco....obcdo.",
        "..oaao....obbco.",
        "...oo..s...oao..",
        "........ooo.o...",
        ".......obcdo....",
        "...ooooobbcdo...",
        "..obbcbbabbcdo..",
        ".obbabbbbbbbcdo.",
        ".oaabbabbbbbbco.",
        ".oaaaaabbabbbbo.",
        "..ooaaaaaaaaoo..",
        "....oooooooo....",
    ]),
    # A teal metal ingot with glowing purple veins, shaped like vanilla ingots
    "arcane_alloy": ({
        "o": (14, 30, 38), "e": (28, 64, 76), "m": (48, 112, 124), "l": (88, 168, 172),
        "h": (160, 228, 224), "w": (236, 255, 252), "p": (138, 70, 214), "q": (206, 150, 255),
    }, [
        "................",
        "................",
        "................",
        "................",
        ".......ooooooo..",
        "......ohhwhhhlo.",
        ".....ohlqllhlmo.",
        "....ohllplhlmmo.",
        "...ohhhllllmmeo.",
        "..oooooooooomeo.",
        "..olllqlllloeeo.",
        "..ollpllqmloeo..",
        "..omllllmmmoo...",
        "..oeeeeeeeeo....",
        "..oooooooooo....",
        "................",
    ]),
    # A small circuit board: gold traces and pins around a glowing purple crystal chip
    "resonant_circuit": ({
        "o": (10, 34, 18), "g": (26, 80, 42), "h": (40, 112, 60), "j": (66, 146, 84),
        "y": (214, 170, 58), "z": (255, 226, 120), "k": (26, 24, 34), "c": (54, 50, 66),
        "p": (138, 70, 214), "q": (206, 150, 255), "w": (245, 232, 255),
    }, [
        "................",
        "...y..y..y..y...",
        "..oooooooooooo..",
        "..ojjjjjjjjjho..",
        "..ojzyyyhhyyho..",
        "y.ojhhhyhhyhgo.y",
        "..ohhkkkkkkhgo..",
        "..oyykcpqckyyo..",
        "y.ohhkcqwckhgo.y",
        "..ohhkcppckhgo..",
        "..ohhkkkkkkhgo..",
        "y.ohyhhhhhhygo.y",
        "..ohyyyyhyyygo..",
        "..ogggggggggoo..",
        "..oooooooooooo..",
        "...y..y..y..y...",
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
    # Preview: the three textures side by side at 8x on a mid-grey background, like an inventory slot
    scale, gap = 8, 8
    width = len(TEXTURES) * 16 * scale + (len(TEXTURES) + 1) * gap
    height = 16 * scale + 2 * gap
    sheet = [[(139, 139, 139, 255)] * width for _ in range(height)]
    for i, name in enumerate(TEXTURES):
        px = pixels_of(name)
        ox = gap + i * (16 * scale + gap)
        for y in range(16 * scale):
            for x in range(16 * scale):
                p = px[y // scale][x // scale]
                if p[3]:
                    sheet[gap + y][ox + x] = p
    with open(sys.argv[2], "wb") as f:
        f.write(encode(sheet, (width, height)))
    print("preview", sys.argv[2])
