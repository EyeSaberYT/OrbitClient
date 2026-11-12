"""Checks every mixin against the Minecraft version it is built for, without starting the game.

Mixins name their targets in strings, so a renamed or removed method still compiles and only fails when
Minecraft starts. For each version this reads the sources Stonecutter generated for it
(versions/<v>/build/generated/stonecutter, run `gradlew :<v>:stonecutterGenerate` first) and looks each
target class, method, field, shadow and @At target up in that version's Mojang-named Minecraft jar.

    python tools/check_mixins.py              every version with generated sources
    python tools/check_mixins.py 1.20.1 26.3  only these
"""
import json
import re
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LOOM = Path.home() / '.gradle/caches/fabric-loom/minecraftMaven/net/minecraft'


def minecraft_jar(version):
    """The Mojang-named jar Loom made for this version (the newest one if there are several)."""
    if (LOOM / 'minecraft-merged-deobf' / version).is_dir():  # 26.1+: shipped unobfuscated
        candidates = list((LOOM / 'minecraft-merged-deobf' / version).glob('*.jar'))
    else:
        candidates = [jar for folder in (LOOM / 'minecraft-merged').glob(f'{version}-loom.mappings.*')
                      for jar in folder.glob('*.jar') if 'sources' not in jar.name]
    candidates = [jar for jar in candidates if 'sources' not in jar.name]
    return max(candidates, key=lambda jar: jar.stat().st_mtime) if candidates else None


def read_class(data):
    """Field and method names, superclass and interfaces of a class file."""
    pos = 10
    pool = [None]
    count = struct.unpack_from('>H', data, 8)[0]
    while len(pool) < count:
        tag = data[pos]
        if tag == 1:  # UTF-8
            size = struct.unpack_from('>H', data, pos + 1)[0]
            pool.append(data[pos + 3:pos + 3 + size].decode('utf-8', 'replace'))
            pos += 3 + size
            continue
        if tag == 7:  # class: index of its name
            pool.append(('class', struct.unpack_from('>H', data, pos + 1)[0]))
        else:
            pool.append(None)
        pos += {3: 5, 4: 5, 5: 9, 6: 9, 7: 3, 8: 3, 9: 5, 10: 5, 11: 5, 12: 5, 15: 4, 16: 3, 17: 5, 18: 5, 19: 3, 20: 3}[tag]
        if tag in (5, 6):  # long and double take two slots
            pool.append(None)
    class_name = lambda index: pool[pool[index][1]].replace('/', '.') if index else None
    super_index, interface_count = struct.unpack_from('>HH', data, pos + 4)
    supers = [class_name(super_index)] + [class_name(struct.unpack_from('>H', data, pos + 8 + 2 * i)[0])
                                          for i in range(interface_count)]
    pos += 8 + 2 * interface_count
    members = {}  # name: descriptors
    for _ in range(2):  # fields, then methods
        member_count = struct.unpack_from('>H', data, pos)[0]
        pos += 2
        for _ in range(member_count):
            name, descriptor = struct.unpack_from('>HH', data, pos + 2)
            members.setdefault(pool[name], set()).add(pool[descriptor])
            attribute_count = struct.unpack_from('>H', data, pos + 6)[0]
            pos += 8
            for _ in range(attribute_count):
                pos += 6 + struct.unpack_from('>I', data, pos + 2)[0]
    return members, [s for s in supers if s]


class Classes:
    """Members of the classes in one jar, looked up through superclasses."""

    def __init__(self, jar):
        self.zip = zipfile.ZipFile(jar)
        self.cache = {}

    def info(self, cls):
        if cls not in self.cache:
            try:
                self.cache[cls] = read_class(self.zip.read(cls.replace('.', '/') + '.class'))
            except KeyError:
                self.cache[cls] = None
        return self.cache[cls]

    def has(self, cls, member, seen=None, descriptor=None, inherited=True):
        """True/False, or None when the class isn't in Minecraft (a library or Fabric class)."""
        info = self.info(cls)
        if info is None:
            return None
        members, supers = info
        if member in members and (descriptor is None or descriptor in members[member]):
            return True
        if not inherited:
            return False
        seen = seen or set()
        for parent in supers:
            if parent in seen or parent.startswith('java.'):
                continue
            seen.add(parent)
            if self.has(parent, member, seen, descriptor):
                return True
        return False


def active_mixins(gen):
    config_file = gen.parent / 'resources/orbitclient.mixins.json'
    config = json.loads(re.sub(r'\$\{[^}]*\}', '0', config_file.read_text()))
    names = config.get('client', []) + config.get('mixins', [])
    plugin = gen / 'com/orbitclient/mixin/OrbitMixinPlugin.java'
    if plugin.exists():
        code = strip_comments(plugin.read_text(encoding='utf-8'))
        names += re.findall(r'mixins\.add\("(\w+)"\)', code)
        # Mixins the plugin turns off for this version (shouldApplyMixin: endsWith(".Name") ... return false).
        skipped = set(re.findall(r'endsWith\("\.(\w+)"\)\)\s*return false', code))
        names = [n for n in names if n not in skipped]
    return names


def strip_comments(code):
    """Drops comments, so code Stonecutter commented out for this version is ignored."""
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', code, flags=re.S)


def resolve(simple, imports, package):
    """A class name as written in the source, to its binary name."""
    if simple[0].islower() and '.' in simple:
        # Written out in full (net.minecraft.client.gui.Gui); nested classes follow the first capitalized part.
        parts = simple.split('.')
        first = next((i for i, p in enumerate(parts) if p[0].isupper()), len(parts))
        return '.'.join(parts[:first + 1]) + ''.join('$' + p for p in parts[first + 1:])
    outer, _, inner = simple.partition('.')
    base = imports.get(outer)
    if base is None:
        return None if outer[0].islower() else f'{package}.{outer}'
    return base + ('$' + inner.replace('.', '$') if inner else '')


def owner_of(descriptor):
    m = re.match(r'L([\w/$]+);', descriptor)
    return m.group(1).replace('/', '.') if m else None


def active_version():
    return re.search(r"stonecutter\.active\s+'([^']+)'", (ROOT / 'stonecutter.gradle').read_text()).group(1)


def check(version):
    # The active version compiles straight from src/; its generated copy can be stale.
    gen = ROOT / ('src/main/java' if version == active_version() else f'versions/{version}/build/generated/stonecutter/main/java')
    jar = minecraft_jar(version)
    if not gen.is_dir() or jar is None:
        return [f'skipped: {"no generated sources" if not gen.is_dir() else "no Minecraft jar in the Loom cache"}']
    classes = Classes(jar)
    problems = []
    for mixin in active_mixins(gen):
        path = gen / 'com/orbitclient/mixin' / f'{mixin}.java'
        if not path.exists():
            problems.append(f'{mixin}: listed but the class has no code for this version')
            continue
        code = strip_comments(path.read_text(encoding='utf-8'))
        imports = {i.split('.')[-1]: i for i in re.findall(r'^import\s+([\w.]+);', code, re.M)}
        package = re.search(r'^package\s+([\w.]+);', code, re.M).group(1)
        head = re.search(r'@Mixin\((.*?)\)\s*(?:public\s+)?(?:abstract\s+)?(?:class|interface)', code, re.S)
        if not head:
            problems.append(f'{mixin}: no @Mixin annotation for this version')
            continue
        targets = [resolve(t, imports, package) for t in re.findall(r'([\w.]+)\.class', head.group(1))]
        targets += [t.replace('/', '.') for t in re.findall(r'"([\w./$]+)"', head.group(1))]
        targets = [t for t in targets if t]
        for target in targets:
            if classes.has(target, '__class__') is None:
                problems.append(f'{mixin}: target class {target} does not exist')
        wanted = []
        for group in re.findall(r'method\s*=\s*(\{[^}]*\}|"[^"]*")', code):
            for selector in re.findall(r'"([^"]+)"', group):
                name, paren, rest = selector.partition('(')
                wanted.append(('method', name))
                if paren:  # a full signature: that exact overload has to exist
                    found = [classes.has(t, name, descriptor='(' + rest, inherited=False) for t in targets]
                    if not any(found) and any(f is not None for f in found):
                        problems.append(f'{mixin}: no overload {selector}')
        wanted += [('accessor', n) for n in re.findall(r'@(?:Accessor|Invoker)\(\s*(?:value\s*=\s*)?"([^"]+)"', code)]
        for decl in re.findall(r'@Shadow(?:\([^)]*\))?\s+((?:@\w+\s+)*(?:[\w<>\[\],.? ]+?\s+)*?([\w$]+)\s*[;(=])', code):
            wanted.append(('shadow', decl[1]))
        for name_kind, name in wanted:
            if name in ('<init>', '<clinit>'):
                continue
            # Injectors only reach methods the target declares itself; shadows and accessors also reach inherited ones.
            found = [classes.has(t, name, inherited=name_kind != 'method') for t in targets]
            if not any(found) and any(f is not None for f in found):
                problems.append(f'{mixin}: {name_kind} "{name}" not in {", ".join(t.split(".")[-1] for t in targets)}')
        for desc in re.findall(r'target\s*=\s*"([^"]+)"', code):
            owner = owner_of(desc)
            rest = re.sub(r'^L[\w/$]+;', '', desc)
            member = re.split(r'[(:]', rest)[0]
            descriptor = rest[len(member):].lstrip(':') or None
            if owner and member:
                found = classes.has(owner, member, descriptor=descriptor)
                if found is False:
                    exists = classes.has(owner, member)
                    problems.append(f'{mixin}: @At target {owner.split(".")[-1]}.{member}'
                                    + (f' has no overload {descriptor}' if exists else ' does not exist'))
                elif found is None and owner.startswith(('net.minecraft', 'com.mojang')):
                    problems.append(f'{mixin}: @At target class {owner} does not exist')
    return problems


def version_key(version):
    return [int(p) for p in version.split('.')]


if __name__ == '__main__':
    versions = sys.argv[1:] or sorted((p.name for p in (ROOT / 'versions').iterdir() if p.is_dir()), key=version_key)
    failed = 0
    for version in versions:
        problems = check(version)
        failed += bool(problems) and not problems[0].startswith('skipped')
        print(f'{version}: {"ok" if not problems else ""}')
        for problem in problems:
            print(f'  {problem}')
    sys.exit(1 if failed else 0)
