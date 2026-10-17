"""Split Create's bundled libraries out of its jar so ForgeGradle can load Create in the dev game.

Create ships Flywheel, Ponder and Registrate inside its jar (META-INF/jarjar). fg.deobf remaps the
outer jar to development names but not jars nested inside it, so those three would load with
production names and crash. This writes, into pack-mods/:
  - each of those libraries as its own jar (build.gradle deobfs them like any other dev mod)
  - create_dev-<version>.jar: Create without them (MixinExtras stays nested: it has no Minecraft
    references, so it needs no remapping)
The original Create jar is left alone and isn't used by the dev game. Run after downloading mods:

    python tools/split_jarjar.py
"""

import glob
import json
import os
import zipfile

ROOT = os.path.join(os.path.dirname(__file__), "..")
PACK_MODS = os.path.join(ROOT, "pack-mods")
SPLIT = ("flywheel", "ponder", "registrate")  # nested jars to pull out (matched case-insensitively)


def split(jar_path, out_name):
    with zipfile.ZipFile(jar_path) as jar:
        meta = json.loads(jar.read("META-INF/jarjar/metadata.json"))
        pulled = [e for e in meta["jars"] if any(s in e["path"].lower() for s in SPLIT)]
        for entry in pulled:
            target = os.path.join(PACK_MODS, os.path.basename(entry["path"]))
            with open(target, "wb") as f:
                f.write(jar.read(entry["path"]))
            print(f"extracted {os.path.basename(target)}")
        meta["jars"] = [e for e in meta["jars"] if e not in pulled]
        skip = {e["path"] for e in pulled} | {"META-INF/jarjar/metadata.json"}
        out = os.path.join(PACK_MODS, out_name)
        with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as dev:
            for item in jar.infolist():
                if item.filename not in skip:
                    dev.writestr(item, jar.read(item.filename))
            if meta["jars"]:
                dev.writestr("META-INF/jarjar/metadata.json", json.dumps(meta, indent=2))
        print(f"wrote {out_name} (kept nested: {', '.join(os.path.basename(e['path']) for e in meta['jars']) or 'none'})")


if __name__ == "__main__":
    jars = glob.glob(os.path.join(PACK_MODS, "create-1.20.1-*.jar"))
    if not jars:
        raise SystemExit("No Create jar in pack-mods/. Run: python tools/download_mods.py --only create")
    original = sorted(jars)[-1]
    version = os.path.basename(original)[len("create-1.20.1-"):-len(".jar")]
    split(original, f"create_dev-1.20.1-{version}.jar")
