"""Launch test: start a real Forge server with the whole mod list plus this mod and the pack's
scripts and config, wait until it's up (or crashes), print a summary, then stop it.

    python tools/full_pack_test.py            # reuse mods already in full-pack/server/mods
    python tools/full_pack_test.py --download # (re)download the mod list first (needs CF_API_KEY)

Everything lives in full-pack/ (git-ignored). Client-only mods (Embeddium, Oculus, Mouse Tweaks, Controlling, Entity Culling) are left out,
since a dedicated server can't load them; check those by launching the pack in a client.
Forge's installer comes from Forge's official maven.
"""

import glob
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.request

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
PACK = os.path.join(ROOT, "full-pack")
SERVER = os.path.join(PACK, "server")
MODS = os.path.join(SERVER, "mods")
FORGE = "1.20.1-47.4.10"
INSTALLER_URL = f"https://maven.minecraftforge.net/net/minecraftforge/forge/{FORGE}/forge-{FORGE}-installer.jar"
CLIENT_ONLY = ("embeddium-", "oculus-", "MouseTweaks-", "Controlling-", "entityculling-")
GRADLEW = os.path.join(ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")


def run(cmd, cwd=ROOT):
    print("> " + " ".join(cmd))
    subprocess.run(cmd, cwd=cwd, check=True)


def prepare(download):
    os.makedirs(MODS, exist_ok=True)
    if download:
        run([sys.executable, os.path.join(ROOT, "tools", "download_mods.py"), MODS])
    for jar in glob.glob(os.path.join(MODS, "*.jar")):
        name = os.path.basename(jar)
        if name.startswith(CLIENT_ONLY):
            os.makedirs(os.path.join(PACK, "client-only"), exist_ok=True)
            shutil.move(jar, os.path.join(PACK, "client-only", name))
        elif name.startswith("skyorigins2core-"):
            os.remove(jar)

    run([GRADLEW, "build", "-x", "test", "--console=plain", "-q"])
    for jar in glob.glob(os.path.join(ROOT, "build", "libs", "skyorigins2core-*.jar")):
        shutil.copy(jar, MODS)

    for src, dst in (("kubejs", "kubejs"), (os.path.join("config", "skyblockbuilder"), os.path.join("config", "skyblockbuilder")),
                     (os.path.join("ftbquests", "quests"), os.path.join("config", "ftbquests", "quests"))):
        shutil.copytree(os.path.join(ROOT, src), os.path.join(SERVER, dst), dirs_exist_ok=True)

    if not os.path.isdir(os.path.join(SERVER, "libraries", "net", "minecraftforge", "forge", FORGE)):
        installer = os.path.join(PACK, "installer", f"forge-{FORGE}-installer.jar")
        if not os.path.exists(installer):
            os.makedirs(os.path.dirname(installer), exist_ok=True)
            print(f"Downloading {INSTALLER_URL}")
            urllib.request.urlretrieve(INSTALLER_URL, installer)
        run(["java", "-jar", installer, "--installServer"], cwd=SERVER)

    with open(os.path.join(SERVER, "eula.txt"), "w") as f:
        f.write("eula=true\n")
    props = os.path.join(SERVER, "server.properties")
    if not os.path.exists(props):
        with open(props, "w") as f:
            f.write("level-type=skyblockbuilder\\:skyblock\nonline-mode=false\nspawn-protection=0\n")


def launch():
    args = os.path.join("libraries", "net", "minecraftforge", "forge", FORGE, "win_args.txt" if os.name == "nt" else "unix_args.txt")
    log = open(os.path.join(SERVER, "console.log"), "w")
    server = subprocess.Popen(["java", "-Xmx6G", "@" + args, "nogui"], cwd=SERVER, stdin=subprocess.PIPE,
                              stdout=log, stderr=subprocess.STDOUT, text=True)
    latest = os.path.join(SERVER, "logs", "latest.log")
    started = time.time()
    outcome = "timed out after 10 minutes"
    while time.time() - started < 600:
        time.sleep(2)
        text = open(os.path.join(SERVER, "console.log"), errors="replace").read()
        if re.search(r"Done \([\d.]+s\)!", text):
            outcome = "started"
            break
        if server.poll() is not None or "Crash report saved" in text:
            outcome = "CRASHED or exited (see full-pack/server/crash-reports and console.log)"
            break
    if server.poll() is None:
        server.stdin.write("stop\n")
        server.stdin.flush()
        try:
            server.wait(120)
        except subprocess.TimeoutExpired:
            server.kill()
    return outcome, latest


def summary(outcome, latest):
    print(f"\nServer {outcome}")
    if not os.path.exists(latest):
        return
    text = open(latest, errors="replace").read()
    mods = len(glob.glob(os.path.join(MODS, "*.jar")))
    print(f"Mods: {mods} jars")
    for pattern in (r"Loaded \d+/\d+ KubeJS server scripts.*", r"Added \d+ recipes.*failed recipes",
                    r"Loaded \d+ chapter groups.*", r"Loaded \d+ island (?:definitions|templates|pools)",
                    r"Loaded template \"[a-z_]+\""):
        source = text
        kubejs = os.path.join(SERVER, "logs", "kubejs", "server.log")
        if "KubeJS" in pattern or "recipes" in pattern:
            source = open(kubejs, errors="replace").read() if os.path.exists(kubejs) else ""
        for line in sorted(set(re.findall(pattern, source))):
            print("  " + line)
    errors = [re.sub(r"^\[[^]]*\] ", "", l)[:200] for l in text.splitlines() if re.search(r"/(ERROR|FATAL)\]", l)]
    print(f"ERROR/FATAL lines: {len(errors)}")
    for line in errors[:20]:
        print("  " + line)


if __name__ == "__main__":
    prepare("--download" in sys.argv)
    summary(*launch())
