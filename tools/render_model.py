"""Tiny isometric renderer for a Minecraft block model JSON, to preview models without the game.

Usage: python render_model.py <model.json> <textures dir (assets/<ns>/textures)> <out.png>
"""
import json
import struct
import sys
import zlib

model_path, tex_root, out_path = sys.argv[1:4]
model = json.load(open(model_path, encoding="utf-8"))


def read_png(path):
    data = open(path, "rb").read()
    pos, idat, w, h = 8, b"", 0, 0
    while pos < len(data):
        n = struct.unpack(">I", data[pos:pos + 4])[0]
        tag, body = data[pos + 4:pos + 8], data[pos + 8:pos + 8 + n]
        if tag == b"IHDR":
            w, h = struct.unpack(">II", body[:8])
        elif tag == b"IDAT":
            idat += body
        pos += 12 + n
    raw, stride, prev, rows, i = zlib.decompress(idat), w * 4, bytearray(w * 4), [], 0
    for _ in range(h):
        f, line = raw[i], bytearray(raw[i + 1:i + 1 + stride]); i += 1 + stride
        for x in range(stride):
            a = line[x - 4] if x >= 4 else 0; b = prev[x]; c = prev[x - 4] if x >= 4 else 0
            if f == 1: line[x] = (line[x] + a) & 255
            elif f == 2: line[x] = (line[x] + b) & 255
            elif f == 3: line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                p = a + b - c; pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c)) & 255
        rows.append(line); prev = line
    return w, h, [[tuple(row[x * 4:x * 4 + 4]) for x in range(w)] for row in rows]


textures = {}
for key, ref in model["textures"].items():
    while isinstance(ref, str) and ref.startswith("#"):
        ref = model["textures"][ref[1:]]
    ns, path = ref.split(":")
    textures[key] = read_png(f"{tex_root}/{path}.png")

S, W, H = 14, 520, 520
img = [[(139, 139, 139)] * W for _ in range(H)]
depth = [[-1e9] * W for _ in range(H)]


def project(x, y, z):
    # view from the south-east, looking down: x right-ish, z toward viewer, y up
    sx = (x - z) * 0.866 * S + W / 2
    sy = ((x + z) * 0.5 - y) * S + H * 0.32
    d = x + z + y  # nearer to the viewer (south-east, above) = larger
    return sx, sy, d


def fill(quad, color, d):
    xs = [p[0] for p in quad]; ys = [p[1] for p in quad]
    for py in range(max(0, int(min(ys))), min(H, int(max(ys)) + 1)):
        for px in range(max(0, int(min(xs))), min(W, int(max(xs)) + 1)):
            inside = True
            for k in range(4):
                (x1, y1), (x2, y2) = quad[k], quad[(k + 1) % 4]
                if (x2 - x1) * (py + 0.5 - y1) - (y2 - y1) * (px + 0.5 - x1) < 0:
                    inside = False; break
            if inside and d >= depth[py][px]:
                depth[py][px] = d; img[py][px] = color


SHADE = {"up": 1.0, "south": 0.8, "east": 0.6}
for el in model["elements"]:
    (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
    glow = "forge_data" in el
    for face, info in el["faces"].items():
        if face not in SHADE:
            continue  # only faces visible from this angle
        w, h, px = textures[info["texture"][1:]]
        u1, v1, u2, v2 = info["uv"]
        cells_u, cells_v = int(abs(u2 - u1)), int(abs(v2 - v1))
        for cu in range(cells_u):
            for cv in range(cells_v):
                tu = int((min(u1, u2) + cu) * w / 16); tv = int((min(v1, v2) + cv) * h / 16)
                r, g, b, a = px[tv][tu]
                if a == 0:
                    continue
                k = 1.0 if glow else SHADE[face]
                color = (int(r * k), int(g * k), int(b * k))
                if face == "up":
                    xa, za = x1 + cu, z1 + cv
                    pts = [(xa, y2, za), (xa + 1, y2, za), (xa + 1, y2, za + 1), (xa, y2, za + 1)]
                elif face == "south":
                    xa, ya = x1 + cu, y2 - cv
                    pts = [(xa, ya, z2), (xa + 1, ya, z2), (xa + 1, ya - 1, z2), (xa, ya - 1, z2)]
                else:  # east
                    za, ya = z2 - cu, y2 - cv
                    pts = [(x2, ya, za), (x2, ya, za - 1), (x2, ya - 1, za - 1), (x2, ya - 1, za)]
                proj = [project(*p) for p in pts]
                quad = [(p[0], p[1]) for p in proj]
                # make winding consistent for the inside test
                area = sum((quad[i][0] * quad[(i + 1) % 4][1] - quad[(i + 1) % 4][0] * quad[i][1]) for i in range(4))
                if area < 0:
                    quad = quad[::-1]
                fill(quad, color, sum(p[2] for p in proj) / 4)

raw = b"".join(b"\x00" + b"".join(struct.pack("BBB", *p) for p in row) for row in img)
def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
open(out_path, "wb").write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", W, H, 8, 2, 0, 0, 0))
                          + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
print("rendered", out_path)
