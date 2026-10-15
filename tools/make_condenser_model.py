"""Build the Void Condenser model: an open frame (base, cap, four pillars) around a floating void orb.

Writes the Minecraft block model JSON and a Blockbench project with the textures embedded, so the
model can be opened and edited in Blockbench. Run from the project root:
    python tools/make_condenser_model.py
"""
import base64
import json
import os
import uuid

ASSETS = "src/main/resources/assets/skyorigins2core"
MODEL_OUT = f"{ASSETS}/models/block/void_condenser.json"
BBMODEL_OUT = "blockbench files/void_condenser.bbmodel"

TEXTURES = ["void_condenser_casing", "void_condenser_top", "void_condenser_bottom", "void_condenser_core"]
CASING, TOP, BOTTOM, CORE = range(4)
SIDES = ("north", "east", "south", "west")

# name, from, to, texture per face (None = face not drawn), whether it glows
ELEMENTS = [
    ("base", [0, 0, 0], [16, 3, 16], {"down": BOTTOM, "up": CASING, **{s: CASING for s in SIDES}}, False),
    ("cap", [0, 13, 0], [16, 16, 16], {"up": TOP, "down": CASING, **{s: CASING for s in SIDES}}, False),
    ("pillar_nw", [0, 3, 0], [3, 13, 3], {s: CASING for s in SIDES}, False),
    ("pillar_ne", [13, 3, 0], [16, 13, 3], {s: CASING for s in SIDES}, False),
    ("pillar_sw", [0, 3, 13], [3, 13, 16], {s: CASING for s in SIDES}, False),
    ("pillar_se", [13, 3, 13], [16, 13, 16], {s: CASING for s in SIDES}, False),
    ("core", [4, 5, 4], [12, 11, 12], {"down": CORE, "up": CORE, **{s: CORE for s in SIDES}}, True),
]


def uv_for(face, f, t):
    """The UVs Minecraft would pick from the element's position, so textures line up across elements."""
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {
        "down": [x1, 16 - z2, x2, 16 - z1],
        "up": [x1, z1, x2, z2],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


def cullface(face, f, t):
    """Faces lying on the block's outer boundary can be skipped when a solid block covers them."""
    on_edge = {
        "down": f[1] == 0, "up": t[1] == 16, "north": f[2] == 0,
        "south": t[2] == 16, "west": f[0] == 0, "east": t[0] == 16,
    }
    return face if on_edge[face] else None


def minecraft_model():
    elements = []
    for name, f, t, faces, glows in ELEMENTS:
        element = {"name": name, "from": f, "to": t, "faces": {}}
        for face, tex in faces.items():
            entry = {"uv": uv_for(face, f, t), "texture": f"#{tex}"}
            cull = cullface(face, f, t)
            if cull:
                entry["cullface"] = cull
            element["faces"][face] = entry
        if glows:
            # Forge extension: render at full brightness even in the dark
            element["forge_data"] = {"block_light": 15, "sky_light": 15}
        elements.append(element)
    textures = {str(i): f"skyorigins2core:block/{name}" for i, name in enumerate(TEXTURES)}
    textures["particle"] = f"skyorigins2core:block/{TEXTURES[CASING]}"
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def blockbench_project():
    textures = []
    for i, name in enumerate(TEXTURES):
        with open(f"{ASSETS}/textures/block/{name}.png", "rb") as f:
            data = base64.b64encode(f.read()).decode()
        textures.append({
            "path": "", "name": f"{name}.png", "folder": "block", "namespace": "skyorigins2core",
            "id": str(i), "particle": i == CASING, "render_mode": "default", "visible": True,
            "mode": "bitmap", "saved": True, "uuid": str(uuid.uuid4()), "relative_path": "",
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
            "source": f"data:image/png;base64,{data}",
        })
    elements = []
    for name, f, t, faces, _ in ELEMENTS:
        element = {
            "name": name, "box_uv": False, "rescale": False, "locked": False, "render_order": "default",
            "allow_mirror_modeling": True, "from": f, "to": t, "autouv": 0, "color": 0,
            "origin": [8, 8, 8], "uuid": str(uuid.uuid4()), "type": "cube", "faces": {},
        }
        for face in ("north", "east", "south", "west", "up", "down"):
            tex = faces.get(face)
            entry = {"uv": uv_for(face, f, t), "texture": tex}
            cull = cullface(face, f, t)
            if cull:
                entry["cullface"] = cull
            element["faces"][face] = entry
        elements.append(element)
    return {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": "void_condenser", "parent": "block/block", "ambientocclusion": True,
        "front_gui_light": False, "visible_box": [1, 1, 0],
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": [e["uuid"] for e in elements],
        "textures": textures,
    }


with open(MODEL_OUT, "w", encoding="utf-8", newline="\n") as f:
    json.dump(minecraft_model(), f, indent=2)
    f.write("\n")
print("wrote", MODEL_OUT)
os.makedirs(os.path.dirname(BBMODEL_OUT), exist_ok=True)
with open(BBMODEL_OUT, "w", encoding="utf-8", newline="\n") as f:
    json.dump(blockbench_project(), f, indent=1)
print("wrote", BBMODEL_OUT)
