#!/usr/bin/env python3
"""Fetch checksum-pinned upstream Rime source dependencies for reproducible native builds."""
from pathlib import Path
import hashlib, tarfile, urllib.request, shutil
ROOT = Path(__file__).resolve().parents[2]
DEST = ROOT / 'engine-rime/src/main/cpp/vendor'
CACHE = ROOT / '.native-downloads'
INPUTS = [
('librime', 'librime-417db2385f732cb0fa194b497042c42abb897d99.tar.gz', 'https://github.com/rime/librime/archive/417db2385f732cb0fa194b497042c42abb897d99.tar.gz', '479cfdccae4caeb2a54867fc1ce9b2f02891c41e2b3bc7faff3e5be84dc70c54'),
('librime/deps/glog', 'glog-7b134a5c82c0c0b5698bb6bf7a835b230c5638e4.tar.gz', 'https://github.com/google/glog/archive/7b134a5c82c0c0b5698bb6bf7a835b230c5638e4.tar.gz', '9c48c7c631be5af3502a19d06ebdb5c2f67d8d0bf216f896848089c1792e6193'),
('librime/deps/yaml-cpp', 'yaml-cpp-2f86d13775d119edbb69af52e5f566fd65c6953b.tar.gz', 'https://github.com/jbeder/yaml-cpp/archive/2f86d13775d119edbb69af52e5f566fd65c6953b.tar.gz', '923b2298148581b14ef21d9ce29d83eca102ff671a547dfdbd227245a199bcd8'),
('librime/deps/leveldb', 'leveldb-99b3c03b3284f5886f9ef9a4ef703d57373e61be.tar.gz', 'https://github.com/google/leveldb/archive/99b3c03b3284f5886f9ef9a4ef703d57373e61be.tar.gz', 'bc87b9bbc5674c91246a89813355e78401759761342cc049e1c3d56350a8a9d1'),
('librime/deps/marisa-trie', 'marisa-trie-3e87d53b78e15f2f43783d5e376561a8c9722051.tar.gz', 'https://github.com/s-yata/marisa-trie/archive/3e87d53b78e15f2f43783d5e376561a8c9722051.tar.gz', 'c24516edc43be8049ef4e23e50a574d4670036fe3595c49c0f01d4d87ce58f57'),
('librime/deps/opencc', 'opencc-e9f3bb3fa1d058fe2fd4f096503222854bdc31ab.tar.gz', 'https://github.com/BYVoid/OpenCC/archive/e9f3bb3fa1d058fe2fd4f096503222854bdc31ab.tar.gz', '903c03e92c18c3573b72431e16af3e641e87911236e9fbe4a562e6fde469e215'),
('boost', 'boost-1.89.0-cmake.tar.xz', 'https://github.com/boostorg/boost/releases/download/boost-1.89.0/boost-1.89.0-cmake.tar.xz', '67acec02d0d118b5de9eb441f5fb707b3a1cdd884be00ca24b9a73c995511f74'),
]
CACHE.mkdir(exist_ok=True)
DEST.mkdir(parents=True, exist_ok=True)
for folder, name, url, digest in INPUTS:
    target = DEST / folder
    marker = target / '.aegis-source'
    if marker.exists() and marker.read_text().strip() == digest:
        continue
    archive = CACHE / name
    if not archive.exists():
        print('Downloading', name, flush=True)
        temporary = CACHE / (name + '.part')
        urllib.request.urlretrieve(url, temporary)
        temporary.replace(archive)
    if hashlib.sha256(archive.read_bytes()).hexdigest() != digest:
        raise RuntimeError('Checksum mismatch: ' + name)
    if target.exists():
        shutil.rmtree(target)
    target.mkdir(parents=True, exist_ok=True)
    with tarfile.open(archive) as bundle:
        for member in bundle.getmembers():
            parts = Path(member.name).parts[1:]
            if not parts:
                continue
            member.name = str(Path(*parts))
            bundle.extract(member, target, filter='data')
    (target / '.aegis-source').write_text(digest + '\n')
    print('Verified', folder, flush=True)
