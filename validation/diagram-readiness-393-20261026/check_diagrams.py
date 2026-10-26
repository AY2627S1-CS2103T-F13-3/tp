"""Inventory maintained DG figures; source freshness is not UML/readability acceptance."""

import argparse
import hashlib
import json
import re
import struct
import subprocess
import zlib
from pathlib import Path


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def text_digest(path):
    return hashlib.sha256(path.read_text(encoding='utf-8').encode('utf-8')).hexdigest()


def png_metadata(path):
    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError(f'Not a PNG: {path}')
    width, height = struct.unpack('>II', data[16:24])
    offset, source = 8, None
    while offset < len(data):
        size = struct.unpack('>I', data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        payload = data[offset + 8:offset + 8 + size]
        if kind == b'zTXt':
            keyword, value = payload.split(b'\0', 1)
            if keyword == b'plantuml':
                source = zlib.decompress(value[1:]).decode('utf-8')
        elif kind == b'iTXt':
            keyword, value = payload.split(b'\0', 1)
            compressed = value[0]
            _, remainder = value[2:].split(b'\0', 1)
            _, value = remainder.split(b'\0', 1)
            if keyword == b'plantuml':
                source = (zlib.decompress(value) if compressed else value).decode('utf-8')
        elif kind == b'tEXt':
            keyword, value = payload.split(b'\0', 1)
            if keyword == b'plantuml':
                source = value.decode('utf-8')
        offset += size + 12
    if source is None:
        raise ValueError(f'No embedded PlantUML source: {path}')
    start = source.index('@startuml')
    end = source.index('@enduml', start) + len('@enduml')
    return width, height, source[start:end].replace('\r\n', '\n').strip()


def inventory(root):
    guide_path = root / 'docs/DeveloperGuide.md'
    guide = guide_path.read_text(encoding='utf-8')
    figures = []
    for source_path in sorted((root / 'docs/diagrams').glob('*.puml')):
        if source_path.name == 'style.puml':
            continue
        image_path = root / 'docs/images' / (source_path.stem + '.png')
        relative_image = 'images/' + image_path.name
        width, height, embedded = png_metadata(image_path)
        source = source_path.read_text(encoding='utf-8').strip()
        comparison = 'exact'
        if embedded != source:
            # Some existing renders inject Smetana into the first raw-source block.
            pragma = '!pragma layout smetana'
            if pragma not in source.splitlines():
                embedded = '\n'.join(line for line in embedded.splitlines() if line != pragma).strip()
                comparison = 'renderer-injected-smetana-removed'
        references = [number for number, line in enumerate(guide.splitlines(), 1)
                      if relative_image in line]
        escaped = re.escape(relative_image)
        full_size = bool(re.search(r'href=[\"\x27]' + escaped + r'[\"\x27]', guide)
                         or re.search(r'\]\(' + escaped + r'\)\]\(' + escaped + r'\)', guide))
        figures.append({
            'name': source_path.stem,
            'source': str(source_path.relative_to(root)).replace('\\', '/'),
            'source_sha256_lf': text_digest(source_path),
            'image': str(image_path.relative_to(root)).replace('\\', '/'),
            'image_sha256': digest(image_path),
            'width': width, 'height': height,
            'embedded_source_matches': embedded == source,
            'comparison': comparison,
            'guide_lines': references,
            'full_size_link': full_size,
        })
    revision = subprocess.check_output(['git', '-C', str(root), 'rev-parse', 'HEAD'], text=True).strip()
    return {
        'repository_revision': revision,
        'guide_sha256_lf': text_digest(guide_path),
        'shared_style_sha256_lf': text_digest(root / 'docs/diagrams/style.puml'),
        'hash_convention': 'UTF-8 text normalized to LF; PNG hashes use exact file bytes.',
        'scope': 'Raw embedded source, dimensions and DG references only; expanded style, semantics and visuals remain.',
        'figures': figures,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    result = inventory(args.root.resolve())
    output = json.dumps(result, indent=2) + '\n'
    if args.output:
        args.output.write_text(output, encoding='utf-8')
    else:
        print(output, end='')
    if not all(item['embedded_source_matches'] and item['guide_lines'] for item in result['figures']):
        raise SystemExit('A figure is stale or missing from the DG; inspect the inventory.')


if __name__ == '__main__':
    main()
