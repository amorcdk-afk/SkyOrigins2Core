"""Convert a Blockbench java_block .bbmodel into a Minecraft block model JSON."""
import json
import sys

src, out, texture_id = sys.argv[1], sys.argv[2], sys.argv[3]
model = json.load(open(src, encoding="utf-8"))
res = model["resolution"]
# Blockbench stores UVs in texture pixels; Minecraft block models use 0-16
su, sv = 16 / res["width"], 16 / res["height"]


def num(value):
    value = round(value, 4)
    return int(value) if value == int(value) else value


elements = []
for e in model["elements"]:
    if e.get("type", "cube") != "cube":
        sys.exit(f"Unsupported element type: {e.get('type')}")
    element = {"from": [num(v) for v in e["from"]], "to": [num(v) for v in e["to"]]}
    if e.get("rotation") and any(e["rotation"]):
        axes = [(axis, angle) for axis, angle in zip("xyz", e["rotation"]) if angle]
        if len(axes) > 1:
            sys.exit(f"Element {e.get('name')} rotates on more than one axis, which block models can't do")
        axis, angle = axes[0]
        element["rotation"] = {"angle": num(angle), "axis": axis, "origin": [num(v) for v in e["origin"]]}
    faces = {}
    for side, face in e["faces"].items():
        if face.get("texture") is None:
            continue
        u1, v1, u2, v2 = face["uv"]
        f = {"uv": [num(u1 * su), num(v1 * sv), num(u2 * su), num(v2 * sv)], "texture": "#0"}
        if face.get("rotation"):
            f["rotation"] = face["rotation"]
        if face.get("cullface"):
            f["cullface"] = face["cullface"]
        faces[side] = f
    element["faces"] = faces
    elements.append(element)

result = {
    "credit": "Made with Blockbench",
    "parent": "minecraft:block/block",
    "render_type": "minecraft:cutout",
    "texture_size": [res["width"], res["height"]],
    "textures": {"0": texture_id, "particle": texture_id},
    "elements": elements,
}
with open(out, "w", encoding="utf-8", newline="\n") as f:
    json.dump(result, f, indent=2)
    f.write("\n")
print(f"wrote {len(elements)} elements to {out}")
