r"""Compile errors from a Gradle log, grouped by Minecraft version and without duplicates.

Gradle prints every javac error twice (as it happens and in the summary), and Stonecutter's paths are long.
Write the log through cmd so PowerShell doesn't wrap its lines:

    cmd /c ".\gradlew.bat :1.20.1:compileJava --continue > build\compile.log 2>&1"
    python tools/compile_errors.py build/compile.log          every version's errors
    python tools/compile_errors.py build/compile.log 1.20.1   counts, and the list for this version only
"""
import collections
import re
import sys

lines = open(sys.argv[1], encoding='utf-8', errors='replace').read().splitlines()
seen = set()
by_version = collections.defaultdict(list)
for i, line in enumerate(lines):
    m = re.search(r'versions\\([\d.]+)\\build\\generated\\stonecutter\\main\\java\\(.*?):(\d+): error: (.*)', line)
    if not m:
        continue
    version, path, number, error = m.groups()
    code = lines[i + 1].strip() if i + 1 < len(lines) else ''
    detail = ' '.join(x.strip() for x in lines[i + 2:i + 5] if any(k in x for k in ('symbol', 'location', 'required', 'found')))
    if (version, path, number) in seen:
        continue
    seen.add((version, path, number))
    by_version[version].append(f'{path.split(chr(92))[-1]}:{number} {error} || {code[:130]} || {detail[:170]}')

only = sys.argv[2] if len(sys.argv) > 2 else None
for version, errors in by_version.items():
    print(f'===== {version}: {len(errors)} errors')
    if only is None or only == version:
        print('\n'.join(errors))
print([line for line in lines if 'BUILD' in line])
