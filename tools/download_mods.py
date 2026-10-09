"""Download the SkyOrigins 2 mod list from CurseForge.

Reads the API key from the CF_API_KEY environment variable (never put the key
in this file). Picks the newest Forge 1.20.1 file of each mod, pulls in their
required dependencies, verifies each download's SHA-1 and saves the jars to
pack-mods/ (or the folder given as the first argument).

Mods whose authors disabled third-party distribution have no API download
link; they are listed at the end with their page URL to download by hand.

Usage (PowerShell):
    $env:CF_API_KEY = "<your key>"
    python tools/download_mods.py [output-folder]
"""

import hashlib
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request

API = "https://api.curseforge.com/v1"
MINECRAFT = 432
MODS_CLASS = 6
FORGE = 1
GAME_VERSION = "1.20.1"
REQUIRED_DEPENDENCY = 3

# CurseForge slugs, from MODLIST.md
MODS = [
    # Skyblock core
    "ex-deorum", "ex-machinis-divitiae-deorum", "skyblock-builder", "sky-guis",
    # Tech
    "create", "mekanism", "mekanism-generators", "mekanism-tools",
    "thermal-foundation", "thermal-expansion", "refined-storage",
    # Magic
    "botania", "ars-nouveau", "occultism", "mystical-agriculture",
    # Dimensions
    "the-twilight-forest", "blue-skies",
    # Progression and scripting
    "ftb-quests-forge", "ftb-teams-forge", "kubejs",
    # QoL and performance
    "jei", "jade", "embeddium", "oculus", "ferritecore", "modernfix",
]


def api_get(path, params=None):
    url = API + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={
        "x-api-key": os.environ["CF_API_KEY"],
        "Accept": "application/json",
        "User-Agent": "skyorigins2core-mod-downloader",
    })
    with urllib.request.urlopen(request) as response:
        return json.load(response)["data"]


def find_mod_by_slug(slug):
    results = api_get("/mods/search", {"gameId": MINECRAFT, "classId": MODS_CLASS, "slug": slug})
    return results[0] if results else None


def latest_forge_file(mod_id):
    files = api_get(f"/mods/{mod_id}/files", {
        "gameVersion": GAME_VERSION, "modLoaderType": FORGE, "pageSize": 50,
    })
    files = [f for f in files if f.get("isAvailable", True)]
    return max(files, key=lambda f: f["fileDate"]) if files else None


def sha1_of(file_info):
    for h in file_info.get("hashes", []):
        if h["algo"] == 1:  # 1 = SHA-1
            return h["value"]
    return None


def download(file_info, out_dir):
    target = os.path.join(out_dir, file_info["fileName"])
    expected = sha1_of(file_info)
    if os.path.exists(target) and expected:
        with open(target, "rb") as existing:
            if hashlib.sha1(existing.read()).hexdigest() == expected:
                return "already downloaded"
    request = urllib.request.Request(file_info["downloadUrl"],
                                     headers={"User-Agent": "skyorigins2core-mod-downloader"})
    with urllib.request.urlopen(request) as response:
        data = response.read()
    if expected and hashlib.sha1(data).hexdigest() != expected:
        raise ValueError("SHA-1 mismatch, file not saved")
    with open(target, "wb") as f:
        f.write(data)
    return f"{len(data) / 1_048_576:.1f} MB"


def main():
    if not os.environ.get("CF_API_KEY"):
        sys.exit("Set the CF_API_KEY environment variable first.")
    out_dir = sys.argv[1] if len(sys.argv) > 1 else "pack-mods"
    os.makedirs(out_dir, exist_ok=True)

    queue = []
    not_found = []
    for slug in MODS:
        mod = find_mod_by_slug(slug)
        if mod:
            queue.append((mod["id"], mod["name"], mod["links"]["websiteUrl"], "list"))
        else:
            not_found.append(slug)

    done = set()
    manual = []
    failed = []
    while queue:
        mod_id, name, page, reason = queue.pop(0)
        if mod_id in done:
            continue
        done.add(mod_id)

        file_info = latest_forge_file(mod_id)
        if not file_info:
            failed.append(f"{name}: no Forge {GAME_VERSION} file")
            continue

        for dep in file_info.get("dependencies", []):
            if dep["relationType"] == REQUIRED_DEPENDENCY and dep["modId"] not in done:
                dep_mod = api_get(f"/mods/{dep['modId']}")
                queue.append((dep_mod["id"], dep_mod["name"], dep_mod["links"]["websiteUrl"],
                              f"needed by {name}"))

        if not file_info.get("downloadUrl"):
            manual.append(f"{name} ({file_info['fileName']}): {page}")
            continue
        try:
            status = download(file_info, out_dir)
            label = "" if reason == "list" else f"  [{reason}]"
            print(f"OK   {file_info['fileName']}  ({status}){label}")
        except (urllib.error.URLError, ValueError) as error:
            failed.append(f"{name}: {error}")

    if not_found:
        print("\nSlugs not found on CurseForge (fix the slug in MODS):")
        for slug in not_found:
            print("  " + slug)
    if manual:
        print("\nDistribution disabled by the author - download these by hand:")
        for line in manual:
            print("  " + line)
    if failed:
        print("\nFailed:")
        for line in failed:
            print("  " + line)
    print(f"\nSaved to {os.path.abspath(out_dir)}")


if __name__ == "__main__":
    main()
