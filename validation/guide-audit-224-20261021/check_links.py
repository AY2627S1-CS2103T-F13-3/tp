"""Audit a locked Jekyll render against this checkout; run after building /tp."""

import json
import re
import subprocess
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[2]
SITE = ROOT / "docs/_site"
OUT = Path(__file__).parent
BASEURL = "/tp"


class Page(HTMLParser):
    def __init__(self, path):
        super().__init__()
        self.ids, self.links, self.images, self.tables = set(), [], [], []
        self.table = self.cells = None
        self.feed(path.read_text(encoding="utf-8"))

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            self.ids.add(attrs["id"])
        for key in ("href", "src"):
            if key in attrs:
                self.links.append(attrs[key])
        if tag == "img":
            self.images.append(attrs.get("src", ""))
        if tag == "table":
            self.table = []
        if tag == "tr" and self.table is not None:
            self.cells = 0
        if tag in ("td", "th") and self.cells is not None:
            self.cells += int(attrs.get("colspan", 1))

    def handle_endtag(self, tag):
        if tag == "tr" and self.cells is not None:
            self.table.append(self.cells)
            self.cells = None
        if tag == "table":
            self.tables.append(self.table)
            self.table = None


pages = {p.resolve(): Page(p) for p in SITE.rglob("*.html")}
if not pages:
    raise SystemExit("Build docs/_site with the locked Jekyll workflow first")
failures, local_count, image_count = [], 0, 0
for path, page in pages.items():
    for href in page.links:
        url = urlsplit(href)
        if url.scheme or url.netloc:
            continue
        target = unquote(url.path)
        if target == BASEURL or target.startswith(BASEURL + "/"):
            target = target[len(BASEURL):] or "/"
        dest = SITE / target.lstrip("/") if target.startswith("/") else path.parent / target
        if not target:
            dest = path
        if dest.is_dir():
            dest /= "index.html"
        local_count += 1
        if href in page.images:
            image_count += 1
        if not dest.exists():
            failures.append(f"{path.name}: missing {href}")
        elif url.fragment and dest.resolve() in pages:
            if unquote(url.fragment) not in pages[dest.resolve()].ids:
                failures.append(f"{path.name}: missing fragment {href}")
    for i, table in enumerate(page.tables, 1):
        if not table or len(set(table)) != 1:
            failures.append(f"{path.name}: inconsistent columns in table {i}: {table}")

source_refs, namespace_refs, readme_count = [], [], 0
for path in [ROOT / "README.md", *sorted((ROOT / "docs").glob("*.md"))]:
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        for target in re.findall(r"https://github\.com/AY2627S1-CS2103T-F13-3/tp/blob/master/([^\s)\"#]+)", line):
            source_refs.append(target)
            if not (ROOT / unquote(target)).is_file():
                failures.append(f"{path.name}:{number}: missing source {target}")
            if "seedu/address" in target:
                namespace_refs.append(f"{path.relative_to(ROOT).as_posix()}\t{number}\t{target}")
        if path.name != "README.md":
            continue
        for target in re.findall(r"\]\(([^)]+)\)", line):
            url = urlsplit(target)
            if url.scheme or url.netloc:
                continue
            readme_count += 1
            dest = ROOT / unquote(url.path)
            if not dest.is_file():
                failures.append(f"README:{number}: missing {target}")
            elif url.fragment and dest.suffix == ".md":
                rendered = (SITE / dest.name).with_suffix(".html").resolve()
                if rendered not in pages or unquote(url.fragment) not in pages[rendered].ids:
                    failures.append(f"README:{number}: missing fragment {target}")

diagram_pairs = []
for source in sorted((ROOT / "docs/diagrams").glob("*.puml")):
    if source.name == "style.puml":
        continue
    rendered = ROOT / "docs/images" / (source.stem + ".png")
    referenced = any(Path(urlsplit(link).path).name == source.stem + ".png"
                     for page in pages.values() for link in page.images)
    diagram_pairs.append({"source": source.name, "image_exists": rendered.is_file(), "embedded": referenced})
    if referenced and not rendered.is_file():
        failures.append(f"Missing diagram image for {source.name}")

report = {
    "source_sha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
    "baseurl": BASEURL,
    "rendered_pages": len(pages),
    "local_targets": local_count,
    "image_references": image_count,
    "tables": sum(len(p.tables) for p in pages.values()),
    "readme_local_targets": readme_count,
    "source_references": len(source_refs),
    "unique_source_paths": len(set(source_refs)),
    "namespace_link_occurrences": len(namespace_refs),
    "diagram_pairs": diagram_pairs,
    "failures": failures,
    "limits": "Local targets/source existence only; browser, external HTTP and runtime checks are separate.",
}
(OUT / "links.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
(OUT / "namespace-links.tsv").write_text("guide\tline\tcurrent_source_path\n" + "\n".join(namespace_refs) + "\n",
                                        encoding="utf-8", newline="\n")
print(json.dumps(report, indent=2))
raise SystemExit(bool(failures))
