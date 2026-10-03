"""Rebuild the bundled worldgen resources from the verified official 1.21.1 JAR.
Usage: python tools/generate_worldgen.py [--client-jar PATH]
The client JAR is cached under build/; normal Gradle builds use checked-in JSON.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SHA1 = "30c73b1c5da787909b2f73340419fdf13b9def88"
URL = f"https://piston-data.mojang.com/v1/objects/{SHA1}/client.jar"
BASE = "data/minecraft/worldgen/"


def binary(kind, a, b):
    return {"type": f"minecraft:{kind}", "argument1": a, "argument2": b}


def rewrite(node, prefix):
    names = {"continents", "offset", "factor", "jaggedness", "depth", "sloped_cheese"}
    if isinstance(node, str):
        for name in names:
            if node == f"minecraft:overworld/{name}":
                return f"{prefix}/{name}"
    if isinstance(node, list):
        return [rewrite(value, prefix) for value in node]
    if isinstance(node, dict):
        return {key: rewrite(value, prefix) for key, value in node.items()}
    return node


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--client-jar", type=Path)
    args = parser.parse_args()
    jar = args.client_jar or ROOT / "build/vanilla-1.21.1-client.jar"
    if not jar.exists():
        if args.client_jar:
            parser.error(f"Missing client JAR: {jar}")
        jar.parent.mkdir(parents=True, exist_ok=True)
        with urllib.request.urlopen(URL, timeout=60) as response:
            payload = response.read()
        if hashlib.sha1(payload).hexdigest() != SHA1:
            raise ValueError("Official client JAR SHA-1 mismatch")
        jar.write_bytes(payload)
    payload = jar.read_bytes()
    if hashlib.sha1(payload).hexdigest() != SHA1:
        raise ValueError("Client JAR is not the official Minecraft 1.21.1 artifact")
    with zipfile.ZipFile(io.BytesIO(payload)) as archive:
        settings = json.loads(archive.read(BASE + "noise_settings/overworld.json"))
        templates = {name: json.loads(archive.read(BASE + f"density_function/overworld/{name}.json"))
                     for name in ("offset", "factor", "jaggedness", "depth", "sloped_cheese")}
    out = ROOT / "src/main/resources/data/continents/worldgen"
    for c in range(5, 101, 5):
        t = c / 100.0
        prefix = f"continents:continentalness_{c}"
        shifted = {"type": "minecraft:shifted_noise", "noise": "minecraft:continentalness",
                   "shift_x": "minecraft:shift_x", "shift_y": 0.0, "shift_z": "minecraft:shift_z",
                   "xz_scale": 0.25 * (1 - 0.65 * t), "y_scale": 0.0}
        detail = {"type": "minecraft:noise", "noise": "minecraft:continentalness",
                  "xz_scale": 1.5, "y_scale": 0.0}
        raw = {"type": "minecraft:flat_cache", "argument": binary("add", -0.38 * t,
               binary("add", shifted, binary("mul", 0.05 * t, detail)))}
        k = 1 - 0.62 * t
        continents = {"type": "minecraft:range_choice", "input": f"{prefix}/crudo",
                      "min_inclusive": -10.0, "max_exclusive": -0.9,
                      "when_in_range": binary("add", -0.9 * (1 - k), binary("mul", f"{prefix}/crudo", k)),
                      "when_out_of_range": f"{prefix}/crudo"}
        functions = {"crudo": raw, "continents": continents,
                     **{name: rewrite(value, prefix) for name, value in templates.items()}}
        for name, value in functions.items():
            write(out / f"density_function/continentalness_{c}/{name}.json", value)
        write(out / f"noise_settings/continentalness_{c}.json", rewrite(settings, prefix))
    print("Generated 20 noise_settings and 140 density_functions from official Minecraft 1.21.1.")


if __name__ == "__main__":
    main()
