"""Check the served UG/DG against the local Jekyll output and local targets."""

from hashlib import sha256
from html.parser import HTMLParser
import json
from pathlib import Path
import re
import subprocess
from urllib.parse import unquote, urljoin, urlsplit
from urllib.request import urlopen


class Page(HTMLParser):
    def __init__(self, content):
        super().__init__()
        self.ids = set()
        self.links = []
        self.feed(content)

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if "id" in attrs:
            self.ids.add(attrs["id"])
        for key in ("href", "src"):
            if key in attrs:
                self.links.append(attrs[key])


root = Path(__file__).resolve().parents[3]
site = root / "docs" / "_site"
base = "http://127.0.0.1:8878/tp/"
pages = {}
report = {
    "source_commit": subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=root, text=True
    ).strip(),
    "base_url": base,
    "pages": [],
    "errors": [],
}
for name in ("UserGuide.html", "DeveloperGuide.html"):
    local = (site / name).read_bytes()
    with urlopen(base + name) as response:
        served = response.read()
    info = {
        "page": name,
        "bytes": len(local),
        "sha256": sha256(local).hexdigest(),
        "matches_local_render": served == local,
        "local_targets_checked": 0,
        "broken_targets": [],
    }
    if served != local:
        report["errors"].append(f"{name}: served bytes differ")
    page = Page(local.decode("utf-8"))
    for href in page.links:
        url = urlsplit(urljoin(base + name, href))
        if url.netloc != urlsplit(base).netloc:
            continue
        if not url.path.startswith("/tp/"):
            info["broken_targets"].append(href)
            continue
        target = site / unquote(url.path.removeprefix("/tp/"))
        if target.is_dir():
            target /= "index.html"
        info["local_targets_checked"] += 1
        if not target.exists():
            info["broken_targets"].append(href)
        elif url.fragment and target.suffix == ".html":
            if target not in pages:
                pages[target] = Page(target.read_text(encoding="utf-8"))
            if unquote(url.fragment) not in pages[target].ids:
                info["broken_targets"].append(href)
    report["errors"].extend(
        f"{name}: missing {href}" for href in info["broken_targets"]
    )
    report["pages"].append(info)

dg = (site / "DeveloperGuide.html").read_text(encoding="utf-8")
section = dg.split('<h3 id="command-name-completion">', 1)[1].split("<h3", 1)[0]
items = re.findall(r"<li>(.*?)</li>", section, re.S)
report["manual_check_items"] = len(items)
report["step_2_code_spans"] = len(re.findall(r"<code\b", items[1]))
report["step_2_stray_backticks"] = "`" in items[1]
if len(items) != 4 or report["step_2_code_spans"] != 6 or "`" in items[1]:
    report["errors"].append("Completion manual checks have unexpected markup")
report["source_files"] = {
    str(path.relative_to(root)).replace("\\", "/"): sha256(path.read_bytes()).hexdigest()
    for path in (
        root / "docs" / "UserGuide.md",
        root / "docs" / "DeveloperGuide.md",
        root / "docs" / "Gemfile.lock",
        root / "docs" / "_config.yml",
        site / "assets" / "main.css",
    )
}
Path(__file__).with_name("render-checks.json").write_text(
    json.dumps(report, indent=2) + "\n", encoding="utf-8"
)
print(json.dumps(report, indent=2))
assert not report["errors"], report["errors"]
