"""Упаковывает готовый боевой JAR и Linux Java без компиляции исходников."""
import copy
import hashlib
import io
import json
import os
from pathlib import Path, PurePosixPath
import tarfile
import tempfile

ROOT = Path(__file__).resolve().parents[2]
JAR = ROOT / 'desktop/build/battle-jar/HeroicCivsBattle.jar'
RUNTIME = ROOT / 'desktop/build/battle-linux-downloads/OpenJDK17U-jre_x64_linux_hotspot_17.0.21_9.tar.gz'
OUTPUT = ROOT / 'desktop/build/distributions/HeroicCivsBattle-Linux64.tar.gz'
JAR_SHA256 = '33f5f75d1344c5431ad2b8d147b1bb92d0ad53ed2c3e696da349bb9d83b79c5c'
RUNTIME_SHA256 = '9f41bf88c7900c7097444a1bdeb84146ae692bc08842490ab098063c74bf9a8a'
PREFIX = 'HeroicCivsBattle/'


def digest(path):
    """Считает контрольную сумму без загрузки всего файла в память."""
    result = hashlib.sha256()
    with path.open('rb') as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b''):
            result.update(chunk)
    return result.hexdigest()


def add_bytes(archive, name, data, mode=0o644):
    """Добавляет небольшой файл с явными Unix-правами."""
    entry = tarfile.TarInfo(PREFIX + name)
    entry.size = len(data)
    entry.mode = mode
    archive.addfile(entry, io.BytesIO(data))


def main():
    """Проверяет входы и публикует архив только после успешной упаковки."""
    for path, expected in ((JAR, JAR_SHA256), (RUNTIME, RUNTIME_SHA256)):
        if digest(path) != expected:
            raise RuntimeError('SHA-256 mismatch: ' + str(path))
    if OUTPUT.exists():
        raise RuntimeError('Output already exists: ' + str(OUTPUT))
    launcher = (Path(__file__).parent / 'HeroicCivsBattle.sh').read_text(encoding='utf-8-sig').replace('\r\n', '\n').encode('utf-8')
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(dir=str(OUTPUT.parent), suffix='.tar.gz.part')
    os.close(descriptor)
    try:
        with tarfile.open(temporary, 'w:gz') as target:
            add_bytes(target, 'HeroicCivsBattle.sh', launcher, 0o755)
            jar_entry = tarfile.TarInfo(PREFIX + JAR.name)
            jar_entry.size = JAR.stat().st_size
            jar_entry.mode = 0o644
            with JAR.open('rb') as source:
                target.addfile(jar_entry, source)
            # Копируем tar-записи без распаковки на Windows, сохраняя права и ссылки.
            with tarfile.open(str(RUNTIME), 'r:gz') as source:
                members = source.getmembers()
                roots = {PurePosixPath(member.name).parts[0] for member in members}
                if len(roots) != 1:
                    raise RuntimeError('Expected one runtime root directory')
                runtime_root = roots.pop()
                def mapped(name):
                    path = PurePosixPath(name)
                    if path.is_absolute() or '..' in path.parts or path.parts[0] != runtime_root:
                        raise RuntimeError('Unexpected runtime path: ' + name)
                    return PREFIX + 'runtime/' + '/'.join(path.parts[1:])
                for original in members:
                    if not (original.isfile() or original.isdir() or original.issym() or original.islnk()):
                        raise RuntimeError('Unexpected tar entry: ' + original.name)
                    entry = copy.copy(original)
                    entry.name = mapped(original.name)
                    entry.uid = entry.gid = 0
                    entry.uname = entry.gname = ''
                    if entry.islnk():
                        entry.linkname = mapped(original.linkname)
                    if entry.issym() and PurePosixPath(entry.linkname).is_absolute():
                        raise RuntimeError('Absolute runtime symlink: ' + original.name)
                    if original.isfile():
                        with source.extractfile(original) as data:
                            target.addfile(entry, data)
                    else:
                        target.addfile(entry)
            metadata = {
                'jar_sha256': JAR_SHA256,
                'runtime': 'Eclipse Temurin Linux x64 JRE 17.0.21+9',
                'runtime_sha256': RUNTIME_SHA256,
                'launcher_sha256': hashlib.sha256(launcher).hexdigest(),
                'linux_graphical_launch_verified': False,
            }
            add_bytes(target, 'PACKAGE.json', (json.dumps(metadata, indent=2) + '\n').encode('utf-8'))
            add_bytes(target, 'README.md', (ROOT / 'docs/Developers/Battle-Linux.md').read_bytes())
        os.replace(temporary, str(OUTPUT))
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print(str(OUTPUT))
    print('SHA256: ' + digest(OUTPUT))
    print('Bytes: ' + str(OUTPUT.stat().st_size))


if __name__ == '__main__':
    main()
