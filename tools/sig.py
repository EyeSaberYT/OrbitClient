"""Prints a Minecraft class's members as declared in a version's game jar (javap -p), to port code between versions.

    python tools/sig.py <class> [regex] <version>...
    python tools/sig.py net.minecraft.client.renderer.entity.layers.CapeLayer render 1.21.8 1.21.9

A class name without a package is looked up in the jar (first match). The regex filters member lines.
"""
import pathlib
import re
import subprocess
import sys
import zipfile

sys.path.insert(0, str(pathlib.Path(__file__).parent))
import check_mixins as c

JDK = next((pathlib.Path(__file__).parent.parent / ".jdk").iterdir())
JAVAP = str(JDK / "bin" / ("javap.exe" if sys.platform == "win32" else "javap"))


def find(jar, name):
    if "." in name:
        return name
    hits = sorted(n[:-6].replace("/", ".") for n in zipfile.ZipFile(jar).namelist()
                  if n.endswith("/" + name + ".class") or n.endswith("$" + name + ".class"))
    return hits[0] if hits else None


def main():
    args = sys.argv[1:]
    versions = [a for a in args if re.fullmatch(r"\d+(\.\d+)+", a)]
    rest = [a for a in args if a not in versions]
    name, pattern = rest[0], (rest[1] if len(rest) > 1 else "")
    for version in versions:
        jar = c.minecraft_jar(version)
        cls = find(jar, name)
        print(f"===== {version}: {cls or name + ' (not found)'}")
        if not cls:
            continue
        out = subprocess.run([JAVAP, "-p", "-cp", jar, cls], capture_output=True, text=True).stdout
        for line in out.splitlines()[1:]:
            line = line.strip()
            if line and line != "}" and re.search(pattern, line):
                print("  " + line)


if __name__ == "__main__":
    main()
