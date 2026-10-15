"""Build SkyOrigins 2 Core's block models from the cube lists below.

For each model this writes the Minecraft block model JSON and a Blockbench project (textures embedded)
in "blockbench files/", so models can be opened and edited in Blockbench. Run from the project root:
    python tools/make_models.py
Preview a model without the game: python tools/render_model.py <model.json> <textures dir> <out.png>
"""
import base64
import json
import os
import uuid

ASSETS = "src/main/resources/assets/skyorigins2core"
SIDES = ("north", "east", "south", "west")
ALL = SIDES + ("up", "down")


def faces(tex, only=ALL, **overrides):
    """Face -> texture name for the given faces; keyword overrides set single faces, e.g. up="grass"."""
    result = {face: tex for face in only}
    result.update(overrides)
    return result


# Each element: name, from, to, {face: texture or (texture, uv)}, extras
# extras: glows (full brightness), rotation (Minecraft element rotation dict)
MODELS = {
    # Open machine frame around a floating void orb
    "void_condenser": {
        "particle": "void_condenser_casing",
        "elements": [
            ("base", [0, 0, 0], [16, 3, 16], faces("void_condenser_casing", down="void_condenser_bottom"), {}),
            ("cap", [0, 13, 0], [16, 16, 16], faces("void_condenser_casing", up="void_condenser_top"), {}),
            ("pillar_nw", [0, 3, 0], [3, 13, 3], faces("void_condenser_casing", SIDES), {}),
            ("pillar_ne", [13, 3, 0], [16, 13, 3], faces("void_condenser_casing", SIDES), {}),
            ("pillar_sw", [0, 3, 13], [3, 13, 16], faces("void_condenser_casing", SIDES), {}),
            ("pillar_se", [13, 3, 13], [16, 13, 16], faces("void_condenser_casing", SIDES), {}),
            ("core", [4, 5, 4], [12, 11, 12], faces("void_condenser_core"), {"glows": True}),
        ],
    },
    # A tiny floating island (grass, dirt, stone tapering to a point) with void crystals growing on top
    "void_island_core": {
        "particle": "island_grass_top",
        "elements": [
            # Grass layer: sides use the top rows of the grass side texture, so the green fringe shows
            ("grass", [1, 8, 1], [15, 10, 15], {
                "up": "island_grass_top", "down": "island_dirt",
                **{side: ("island_grass_side", [1, 0, 15, 2]) for side in SIDES},
            }, {}),
            ("dirt", [2, 6, 2], [14, 8, 14], faces("island_dirt"), {}),
            ("stone_upper", [3, 4, 3], [13, 6, 13], faces("island_stone"), {}),
            ("stone_lower", [5, 2, 5], [11, 4, 11], faces("island_stone"), {}),
            ("stone_tip", [7, 0, 7], [9, 2, 9], faces("island_stone"), {}),
            ("crystal", [6, 10, 6], [10, 16, 10], faces("void_crystal", SIDES + ("up",)),
             {"glows": True, "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}}),
            ("shard_west", [3, 10, 4], [5, 13, 6], faces("void_crystal", SIDES + ("up",)), {"glows": True}),
            ("shard_east", [11, 10, 10], [13, 14, 12], faces("void_crystal", SIDES + ("up",)), {"glows": True}),
        ],
    },
}


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


def cullface(face, f, t, rotated):
    """Faces lying on the block's outer boundary can be skipped when a solid block covers them."""
    if rotated:
        return None
    on_edge = {
        "down": f[1] == 0, "up": t[1] == 16, "north": f[2] == 0,
        "south": t[2] == 16, "west": f[0] == 0, "east": t[0] == 16,
    }
    return face if on_edge[face] else None


def face_spec(spec, face, f, t):
    """(texture, uv) for a face, using the position-based UV unless the model overrides it."""
    if isinstance(spec, tuple):
        return spec
    return spec, uv_for(face, f, t)


def texture_list(model):
    names = [model["particle"]]
    for _, _, _, face_map, _ in model["elements"]:
        for spec in face_map.values():
            name = spec[0] if isinstance(spec, tuple) else spec
            if name not in names:
                names.append(name)
    return names


def minecraft_model(model):
    names = texture_list(model)
    elements = []
    for name, f, t, face_map, extras in model["elements"]:
        element = {"name": name, "from": f, "to": t}
        if "rotation" in extras:
            element["rotation"] = extras["rotation"]
        element["faces"] = {}
        for face, spec in face_map.items():
            tex, uv = face_spec(spec, face, f, t)
            entry = {"uv": uv, "texture": f"#{names.index(tex)}"}
            cull = cullface(face, f, t, "rotation" in extras)
            if cull:
                entry["cullface"] = cull
            element["faces"][face] = entry
        if extras.get("glows"):
            # Forge extension: render at full brightness even in the dark
            element["forge_data"] = {"block_light": 15, "sky_light": 15}
        elements.append(element)
    textures = {str(i): f"skyorigins2core:block/{n}" for i, n in enumerate(names)}
    textures["particle"] = f"skyorigins2core:block/{model['particle']}"
    return {"parent": "minecraft:block/block", "textures": textures, "elements": elements}


def blockbench_project(model_name, model):
    names = texture_list(model)
    textures = []
    for i, name in enumerate(names):
        with open(f"{ASSETS}/textures/block/{name}.png", "rb") as f:
            data = base64.b64encode(f.read()).decode()
        textures.append({
            "path": "", "name": f"{name}.png", "folder": "block", "namespace": "skyorigins2core",
            "id": str(i), "particle": name == model["particle"], "render_mode": "default", "visible": True,
            "mode": "bitmap", "saved": True, "uuid": str(uuid.uuid4()), "relative_path": "",
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16,
            "source": f"data:image/png;base64,{data}",
        })
    elements = []
    for name, f, t, face_map, extras in model["elements"]:
        rotation = extras.get("rotation")
        element = {
            "name": name, "box_uv": False, "rescale": False, "locked": False, "render_order": "default",
            "allow_mirror_modeling": True, "from": f, "to": t, "autouv": 0, "color": 0,
            "origin": rotation["origin"] if rotation else [8, 8, 8], "uuid": str(uuid.uuid4()), "type": "cube",
            "faces": {},
        }
        if rotation:
            element["rotation"] = [rotation["angle"] if axis == rotation["axis"] else 0 for axis in "xyz"]
        for face in ALL:
            spec = face_map.get(face)
            if spec is None:
                element["faces"][face] = {"uv": uv_for(face, f, t), "texture": None}
                continue
            tex, uv = face_spec(spec, face, f, t)
            entry = {"uv": uv, "texture": names.index(tex)}
            cull = cullface(face, f, t, rotation is not None)
            if cull:
                entry["cullface"] = cull
            element["faces"][face] = entry
        elements.append(element)
    return {
        "meta": {"format_version": "4.10", "model_format": "java_block", "box_uv": False},
        "name": model_name, "parent": "block/block", "ambientocclusion": True,
        "front_gui_light": False, "visible_box": [1, 1, 0],
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "outliner": [e["uuid"] for e in elements],
        "textures": textures,
    }


os.makedirs("blockbench files", exist_ok=True)
for model_name, model in MODELS.items():
    out = f"{ASSETS}/models/block/{model_name}.json"
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(minecraft_model(model), f, indent=2)
        f.write("\n")
    bb = f"blockbench files/{model_name}.bbmodel"
    with open(bb, "w", encoding="utf-8", newline="\n") as f:
        json.dump(blockbench_project(model_name, model), f, indent=1)
    print("wrote", out, "and", bb)
