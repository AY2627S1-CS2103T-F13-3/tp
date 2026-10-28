#!/usr/bin/env python3
"""Resolve the expected package version from candidate source and the reviewed handoff.

Prepared by Vincent Lin with OpenAI Codex assistance.
"""

import argparse
from pathlib import Path
import re


def resolve_version(build_file, expected_version=None, require_expected=False):
    """Reject missing handoff inputs or a version inconsistent with candidate source."""
    versions = re.findall(r"^version[ \t]*=[ \t]*['\"]([^'\"\r\n]+)['\"][ \t]*$",
                          build_file.read_text(), re.MULTILINE)
    if len(versions) != 1 or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.+_-]*", versions[0]):
        raise ValueError("Candidate build.gradle must declare one literal product version")
    if require_expected and not expected_version:
        raise ValueError("Manual dispatch requires the reviewed candidate's expected version")
    if expected_version and expected_version != versions[0]:
        raise ValueError("Expected version differs from the candidate source product version")
    return versions[0]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--build-file", type=Path, required=True)
    parser.add_argument("--expected-version")
    parser.add_argument("--require-expected", action="store_true")
    args = parser.parse_args()
    print("EXPECTED_VERSION=" + resolve_version(args.build_file, args.expected_version, args.require_expected))


if __name__ == "__main__":
    main()
