"""GUI textures for SkyOrigins 2 Core.

mana_bar.png (64x10): the mana bar beside the hotbar, styled like the vanilla XP bar but blue and shorter.
Rows 0-4 are the empty background, rows 5-9 the fill.

Usage: python tools/gui_art.py src/main/resources/assets/skyorigins2core/textures/gui [preview.png]
"""
import os
import struct
import sys
import zlib

WIDTH, BAR = 64, 5
OUTLINE = (12, 14, 30, 255)
EMPTY_DARK = (26, 32, 58, 255)
EMPTY = (40, 50, 86, 255)
NOTCH = (18, 22, 44, 255)
FILL_DARK = (28, 92, 196, 255)
FILL = (54, 140, 240, 255)
FILL_LIGHT = (126, 196, 255, 255)
NOTCH_FILL = (34, 104, 210, 255)


CLEAR = (0, 0, 0, 0)


def corner(x, y):
    """Rounded ends: 'clear' outside the curve, 'edge' on it, None inside. Distances are from the nearest corner."""
    dx = min(x, WIDTH - 1 - x)
    dy = min(y, BAR - 1 - y)
    if (dx, dy) in ((0, 0), (1, 0), (0, 1)):
        return "clear"
    if (dx, dy) in ((2, 0), (1, 1), (0, 2)):
        return "edge"
    return None


def bar(empty):
    rows = []
    for y in range(BAR):
        row = []
        for x in range(WIDTH):
            shape = corner(x, y)
            if shape == "clear":
                row.append(CLEAR)
                continue
            edge = shape == "edge" or x == 0 or x == WIDTH - 1 or y == 0 or y == BAR - 1
            # Segment notches every 16px (a quarter of the bar), like the dividers on the XP bar
            notch = x % 16 == 0 and 0 < x < WIDTH - 1 and 0 < y < BAR - 1
            if edge:
                row.append(OUTLINE)
            elif empty:
                row.append(NOTCH if notch else EMPTY_DARK if y == BAR - 2 else EMPTY)
            else:
                row.append(NOTCH_FILL if notch else FILL_LIGHT if y == 1 else FILL_DARK if y == BAR - 2 else FILL)
        rows.append(row)
    return rows


def encode(pixels):
    raw = b"".join(b"\x00" + b"".join(struct.pack("BBBB", *p) for p in row) for row in pixels)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    w, h = len(pixels[0]), len(pixels)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


texture = bar(empty=True) + bar(empty=False)
os.makedirs(sys.argv[1], exist_ok=True)
with open(os.path.join(sys.argv[1], "mana_bar.png"), "wb") as f:
    f.write(encode(texture))
print("wrote mana_bar.png")

if len(sys.argv) > 2:
    # Preview at 4x: empty bar, then a 60%-full bar, on a grey background
    scale, pad = 4, 8
    full = bar(empty=True)
    part = [[(bar(empty=False)[y][x] if x < int(WIDTH * 0.6) else full[y][x]) for x in range(WIDTH)] for y in range(BAR)]
    rows = []
    for source in (full, part):
        for y in range(BAR * scale):
            rows.append([(139, 139, 139, 255)] * pad + [source[y // scale][x // scale] for x in range(WIDTH * scale)] + [(139, 139, 139, 255)] * pad)
        rows.extend([[(139, 139, 139, 255)] * (WIDTH * scale + 2 * pad)] * pad)
    with open(sys.argv[2], "wb") as f:
        f.write(encode(rows))
    print("preview", sys.argv[2])
