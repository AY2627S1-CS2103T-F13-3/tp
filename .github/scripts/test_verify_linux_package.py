"""Checks outcome synchronization and UI feedback verification without an X11 display."""

import tempfile
from pathlib import Path
import unittest
from unittest.mock import Mock, patch

import verify_linux_package as verifier


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
