"""Checks candidate versions, package metadata and UI outcome handling without an X11 display."""

import tempfile
from pathlib import Path
import unittest
from unittest.mock import Mock, patch
import zipfile

from resolve_candidate_version import resolve_version
import verify_linux_package as verifier


class CandidateVersionTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.build_file = Path(self.folder.name) / "build.gradle"
        self.build_file.write_text("version = '1.5'\n")

    def test_event_run_uses_source_version_and_tracks_later_candidates(self):
        self.assertEqual("1.5", resolve_version(self.build_file))
        self.build_file.write_text('version = "1.6"\n')
        self.assertEqual("1.6", resolve_version(self.build_file))

    def test_manual_run_accepts_matching_reviewed_version(self):
        self.assertEqual("1.5", resolve_version(self.build_file, "1.5", require_expected=True))

    def test_manual_run_rejects_absent_or_mismatched_handoff_version(self):
        for version in (None, "", "1.4", "v1.5", "1.5\nEXPECTED_SHA256=other"):
            with self.subTest(version=version), self.assertRaises(ValueError):
                resolve_version(self.build_file, version, require_expected=True)

    def test_missing_computed_or_ambiguous_source_version_is_rejected(self):
        for source in ("", "version = releaseVersion\n", "version = '1.5'\nversion = '1.6'\n"):
            self.build_file.write_text(source)
            with self.subTest(source=source), self.assertRaises(ValueError):
                resolve_version(self.build_file, "1.5", require_expected=True)

    def test_source_expectation_accepts_v15_jar_and_rejects_stale_manifest(self):
        jar = Path(self.folder.name) / "ponhub.jar"
        expected = resolve_version(self.build_file, "1.5", require_expected=True)
        for manifest_version in ("1.5", "1.4"):
            with zipfile.ZipFile(jar, "w") as archive:
                archive.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n"
                                 "Implementation-Title: PonHub\n"
                                 f"Implementation-Version: {manifest_version}\n"
                                 "Main-Class: ponhub.Main\n")
                archive.writestr("ponhub/Main.class", b"synthetic metadata fixture")
            if manifest_version == "1.5":
                self.assertEqual("1.5", verifier.verify_package_metadata(jar, expected)["Implementation-Version"])
                with self.assertRaisesRegex(RuntimeError, "Manifest implementation version differs"):
                    verifier.verify_package_metadata(jar, "1.4")
            else:
                with self.assertRaisesRegex(RuntimeError, "Manifest implementation version differs"):
                    verifier.verify_package_metadata(jar, expected)


class PackageSessionTest(unittest.TestCase):
    def setUp(self):
        self.folder = tempfile.TemporaryDirectory()
        self.addCleanup(self.folder.cleanup)
        self.session = verifier.PackageSession.__new__(verifier.PackageSession)
        self.session.log = Path(self.folder.name) / "console.log"
        self.session.log.write_text("")
        self.session.name = "synthetic"
        self.session.transcript = []
        self.session.read_feedback = Mock(return_value="New tutor added: Synthetic Tutor (T1).")
        self.session.wait = lambda predicate, description: predicate()

    def run_with_outcome(self, operation, marker):
        def input_event(*arguments, **options):
            if arguments[-1] == "Return":
                with self.session.log.open("a") as log:
                    log.write(marker)

        with patch.object(verifier, "run", side_effect=input_event), patch.object(verifier.time, "sleep"):
            operation()

    def test_success_uses_safe_marker_and_reads_actual_feedback(self):
        self.run_with_outcome(lambda: self.session.command("add synthetic", "New tutor added:"),
                              "INFO: Command type=add outcome=success\n")
        self.assertEqual("New tutor added: Synthetic Tutor (T1).",
                         self.session.transcript[0]["feedback_from_ui"])
        self.assertNotIn("Synthetic Tutor", self.session.transcript[0]["output"])

    def test_success_marker_does_not_substitute_for_feedback_check(self):
        with self.assertRaisesRegex(RuntimeError, "Unexpected visible result"):
            self.run_with_outcome(lambda: self.session.command("add synthetic", "Incorrect feedback"),
                                  "INFO: Command type=add outcome=success\n")

    def test_rejection_still_fails_a_successful_command(self):
        with self.assertRaisesRegex(RuntimeError, "Command rejected"):
            self.run_with_outcome(lambda: self.session.command("add synthetic"),
                                  "INFO: Command type=add outcome=parse_rejected cause=ParseException\n")

    def test_expected_rejection_accepts_both_categories(self):
        for outcome in ("parse_rejected", "command_rejected"):
            self.run_with_outcome(lambda: self.session.reject("add synthetic"),
                                  f"INFO: Command type=add outcome={outcome} cause=ParseException\n")
            self.assertTrue(self.session.transcript[-1]["rejected"])

    def test_unexpected_success_fails_a_rejection_check(self):
        with self.assertRaisesRegex(RuntimeError, "unexpectedly succeeded"):
            self.run_with_outcome(lambda: self.session.reject("add synthetic"),
                                  "INFO: Command type=add outcome=success\n")


if __name__ == "__main__":
    unittest.main()
