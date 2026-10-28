"""Exercise rendered link checks for deployment and empty-base previews."""

import contextlib
import io
import json
from pathlib import Path
import runpy
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch


class SiteLinksTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.root = Path(self.folder.name)
        (self.root / "tools").mkdir()
        self.script = self.root / "tools/check_site_links.py"
        shutil.copyfile(Path(__file__).with_name("check_site_links.py"), self.script)
        (self.root / "README.md").write_text("", encoding="utf-8")
        (self.root / "docs").mkdir()
        self.site = self.root / "site"
        self.site.mkdir()
        (self.site / "index.html").write_text('<h1 id="home">Home</h1>', encoding="utf-8")
        (self.site / "nested").mkdir()
        (self.site / "nested/Other.html").write_text('<h1 id="other">Other</h1>', encoding="utf-8")

    def check(self, baseurl, links):
        page = '<h1 id="example">Example</h1><h2 id="with space">Encoded</h2>'
        page += "".join(f'<a href="{link}">jump</a>' for link in links)
        (self.site / "DeveloperGuide.html").write_text(page, encoding="utf-8")
        output = io.StringIO()
        arguments = [str(self.script), "--site", str(self.site), "--baseurl", baseurl]
        with patch.object(sys, "argv", arguments), contextlib.redirect_stdout(output):
            # Only Git metadata is stubbed; all link parsing and filesystem checks run.
            with patch("subprocess.check_output", return_value="fixture-source-sha\n"):
                with self.assertRaises(SystemExit) as result:
                    runpy.run_path(str(self.script), run_name="__main__")
        return result.exception.code, json.loads(output.getvalue())

    def test_valid_links_resolve_to_their_own_pages(self):
        for baseurl in ("", "/", "/tp", "/tp/"):
            with self.subTest(baseurl=baseurl):
                prefix = baseurl.rstrip("/")
                links = ["#example", "?preview=1#example", "#with%20space",
                         "DeveloperGuide.html#example", "nested/Other.html#other",
                         f"{prefix}/nested/Other.html#other", f"{prefix}/#home"]
                code, report = self.check(baseurl, links)
                self.assertEqual(0, code, report["failures"])
                self.assertEqual([], report["failures"])
                self.assertEqual(len(links), report["local_targets"])

    def test_broken_targets_and_fragments_still_fail(self):
        for baseurl in ("", "/tp"):
            with self.subTest(baseurl=baseurl):
                links = ["#missing", "#home", "nested/Other.html#example", "Missing.html"]
                code, report = self.check(baseurl, links)
                self.assertEqual(1, code)
                self.assertCountEqual([
                    "DeveloperGuide.html: missing fragment #missing",
                    "DeveloperGuide.html: missing fragment #home",
                    "DeveloperGuide.html: missing fragment nested/Other.html#example",
                    "DeveloperGuide.html: missing Missing.html",
                ], report["failures"])


if __name__ == "__main__":
    unittest.main()
