#!/usr/bin/env python3
"""Exercise the unmodified release JAR through its real command box on X11.

Zhu Zhi Yu used OpenAI Codex for this bounded hosted-Linux verification tool.
Only disposable, fictitious application data is created beneath the evidence folder.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import time
import zipfile


MAX_JAR_BYTES = 100 * 1024 * 1024


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def run(*arguments, **options):
    return subprocess.run(arguments, check=True, text=True, timeout=15, **options)


class PackageSession:
    def __init__(self, folder, evidence, name, transcript, expected_version):
        self.folder = folder
        self.evidence = evidence
        self.name = name
        self.transcript = transcript
        self.expected_version = expected_version
        self.log = evidence / f"{name}.log"
        self.output = self.log.open("w")
        self.process = subprocess.Popen(
            ["java", "-jar", "ponhub.jar"], cwd=folder,
            stdout=self.output, stderr=subprocess.STDOUT,
        )
        self.window = None

    def wait(self, predicate, description):
        deadline = time.monotonic() + 30
        while time.monotonic() < deadline:
            value = predicate()
            if value:
                return value
            require(self.process.poll() is None,
                    f"Application exited before {description}; see {self.log}")
            time.sleep(0.1)
        raise RuntimeError(f"Timed out waiting for {description}; see {self.log}")

    def find_window(self):
        result = subprocess.run(
            ["xdotool", "search", "--onlyvisible", "--all", "--pid",
             str(self.process.pid), "--name", "^PonHub$"],
            capture_output=True, text=True, timeout=5,
        )
        return result.stdout.splitlines()[0] if result.returncode == 0 else None

    def ready(self):
        self.window = self.wait(self.find_window, "a visible production PonHub window")
        expected_message = f"Starting PonHub v{self.expected_version}"
        self.wait(lambda: expected_message if expected_message in self.log.read_text() else None,
                  f"startup log entry {expected_message!r}")
        # The stage is mapped before UiManager finishes filling its controls.
        time.sleep(0.5)
        run("xdotool", "windowfocus", "--sync", self.window)
        geometry = run("xdotool", "getwindowgeometry", "--shell", self.window,
                       capture_output=True).stdout
        bounds = dict(line.split("=", 1) for line in geometry.splitlines() if "=" in line)
        width, height = int(bounds["WIDTH"]), int(bounds["HEIGHT"])
        require(width >= 100 and height >= 100, "PonHub window is too small to focus its command dock")
        # The full-width input sits above the 30-pixel status footer on this X11 display.
        # Locate it relative to the actual client height, not the former top input position.
        self.command_position = (width // 2, height - 50)
        # This check owns a fresh default-layout window: feedback follows the header/navigation row.
        self.feedback_position = (width // 2, 100)
        self.focus_command()
        # Prove focus through a harmless real command before any mutation, including after restart.
        self.command("help", "PonHub commands available in this build:")
        self.screenshot(f"{self.name}-command-ready")

    def focus_command(self):
        run("xdotool", "mousemove", "--window", self.window,
            *(str(value) for value in self.command_position), "click", "1")

    def read_feedback(self):
        """Copy the actual read-only feedback control; production logs deliberately omit its text."""
        sentinel = "Waiting for synthetic package-test feedback"
        run("xclip", "-selection", "clipboard", input=sentinel, stdout=subprocess.DEVNULL)
        run("xdotool", "mousemove", "--window", self.window,
            *(str(value) for value in self.feedback_position), "click", "1")
        run("xdotool", "key", "--clearmodifiers", "ctrl+a", "ctrl+c")

        def copied():
            result = subprocess.run(["xclip", "-selection", "clipboard", "-out"],
                                    capture_output=True, text=True, timeout=5)
            return result.stdout if result.returncode == 0 and result.stdout != sentinel else None

        try:
            return self.wait(copied, "feedback copied from the visible result control")
        finally:
            self.focus_command()

    def command(self, text, expected_feedback=None):
        offset = self.log.stat().st_size
        run("xdotool", "key", "--clearmodifiers", "ctrl+a")
        run("xdotool", "type", "--clearmodifiers", "--delay", "1", text)
        run("xdotool", "key", "Return")

        def completed():
            with self.log.open() as log:
                log.seek(offset)
                output = log.read()
            require(" outcome=parse_rejected" not in output and " outcome=command_rejected" not in output,
                    f"Command rejected: {text}\n{output}")
            require("SEVERE:" not in output, f"Fatal application error: {output}")
            return output if " outcome=success" in output else None

        output = self.wait(completed, f"successful command {text!r}")
        time.sleep(0.2)  # The logic outcome precedes the UI's resulting display pulse.
        feedback = None if text == "exit" else self.read_feedback()
        self.transcript.append({"session": self.name, "command": text, "output": output,
                                "feedback_from_ui": feedback})
        if expected_feedback is not None:
            require(feedback.startswith(expected_feedback),
                    f"Unexpected visible result for {text!r}: {feedback}")

    def reject(self, text):
        offset = self.log.stat().st_size
        run("xdotool", "key", "--clearmodifiers", "ctrl+a")
        run("xdotool", "type", "--clearmodifiers", "--delay", "1", text)
        run("xdotool", "key", "Return")

        def completed():
            with self.log.open() as log:
                log.seek(offset)
                output = log.read()
            require("SEVERE:" not in output, f"Fatal application error: {output}")
            require(" outcome=success" not in output, f"Command unexpectedly succeeded: {text}\n{output}")
            return output if " outcome=parse_rejected" in output or " outcome=command_rejected" in output else None

        output = self.wait(completed, f"rejected command {text!r}")
        time.sleep(0.2)
        self.transcript.append({"session": self.name, "command": text, "rejected": True, "output": output,
                                "feedback_from_ui": self.read_feedback()})

    def screenshot(self, name):
        run("import", "-window", self.window, str(self.evidence / f"{name}.png"))

    def exit(self):
        self.command("exit")
        require(self.process.wait(timeout=15) == 0, "Application shutdown failed")
        self.output.close()

    def cleanup(self):
        if self.process.poll() is None:
            try:
                subprocess.run(["import", "-window", "root",
                                str(self.evidence / f"{self.name}-failure.png")], timeout=10,
                               stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            except (OSError, subprocess.TimeoutExpired):
                pass  # Failure evidence must not prevent process cleanup or the result report.
            self.process.terminate()
            try:
                self.process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                self.process.kill()
                self.process.wait(timeout=5)
        self.output.close()


def read_manifest(jar):
    with zipfile.ZipFile(jar) as archive:
        raw_manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")
        entries = set(archive.namelist())
    attributes = {}
    current_name = None
    for line in raw_manifest.splitlines():
        if line.startswith(" "):
            require(current_name is not None, "Manifest continuation has no preceding attribute")
            attributes[current_name] += line[1:]
        elif line:
            name, separator, value = line.partition(": ")
            require(separator, f"Malformed manifest line: {line}")
            attributes[name] = value
            current_name = name
    return attributes, entries


def verify_package_metadata(jar, expected_version):
    require(jar.name == "ponhub.jar", "Candidate JAR must retain the distribution filename ponhub.jar")
    attributes, entries = read_manifest(jar)
    require(attributes.get("Implementation-Title") == "PonHub", "Unexpected implementation title")
    require(attributes.get("Implementation-Version") == expected_version,
            "Manifest implementation version differs from the release candidate")
    main_class = attributes.get("Main-Class")
    require(main_class is not None, "Manifest does not declare Main-Class")
    require(main_class.replace(".", "/") + ".class" in entries, "Manifest entry point is absent from the JAR")
    return attributes


def verify_state(path, members=("S2",), first_status="absent"):
    state = json.loads(path.read_text())
    require([person["id"] for person in state["people"]] == ["T1", "S1", "S2"],
            "Creation order or stable person IDs differ")
    require(state["personCounters"] == {"STUDENT": 2, "TUTOR": 1, "PARENT": 0},
            "Person allocation counters differ")
    student = state["people"][1]
    require(student["name"] == "Alex Lee" and student["level"] == "P3",
            "Edited Student fields were not retained")
    require(state["lastAllocatedLessonSequence"] == 2, "Lesson allocation counter differs")
    require([lesson["id"] for lesson in state["lessons"]] == ["L1", "L2"],
            "Creation order or stable Lesson IDs differ")
    lesson = state["lessons"][0]
    require(lesson["id"] == "L1" and lesson["tutorId"] == "T1"
            and lesson["enrolledStudentIds"] == list(members), "Shared Lesson membership differs")
    require((lesson["day"], lesson["startTime"], lesson["endTime"], lesson["subject"], lesson["room"])
            == ("Mon", "1630", "1730", "Advanced Math", "R3"), "Edited Lesson fields differ")
    records = {(record["studentId"], record["lessonId"], record["date"], record["status"])
               for record in state["attendance"]}
    require(len(state["attendance"]) == 2 and records == {
        ("S1", "L1", "2026-10-05", first_status),
        ("S2", "L1", "2026-10-05", "absent"),
    }, "Independent/corrected/former-member attendance differs")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--expected-sha256", required=True)
    parser.add_argument("--expected-version", required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=True)
    transcript = []
    session = None
    result = {"passed": False, "source": args.source, "platform": platform.platform(),
              "architecture": platform.machine(), "display": os.environ.get("DISPLAY"),
              "expected_sha256": args.expected_sha256.lower(),
              "expected_version": args.expected_version,
              "os_release": Path("/etc/os-release").read_text(),
              "scope": "Actual packaged-JAR X11 automation; no manual physical-display check"}
    try:
        content = args.jar.read_bytes()
        digest = hashlib.sha256(content).hexdigest()
        result.update(jar_sha256=digest, jar_bytes=len(content))
        require(digest == args.expected_sha256.lower(), "Candidate JAR SHA-256 mismatch")
        require(len(content) <= MAX_JAR_BYTES, "Candidate JAR exceeds the 100 MiB release limit")
        result["manifest"] = verify_package_metadata(args.jar, args.expected_version)
        with (evidence / "runtime.txt").open("w") as runtime:
            run("java", "-XshowSettings:properties", "-version",
                stdout=runtime, stderr=subprocess.STDOUT)
        folder = evidence / "fresh application folder"
        folder.mkdir()
        shutil.copyfile(args.jar, folder / "ponhub.jar")
        data = folder / "data" / "ponhub.json"
        session = PackageSession(folder, evidence, "initial", transcript, args.expected_version)
        session.ready()
        for command in [
            "add r/tutor n/Mei Lim p/92345678 e/mei@example.com",
            "addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1",
            "addlesson d/Mon st/1730 et/1830 s/Science tu/Mei Lim rm/R2",
            "add r/student n/Alex Tan l/S2 pp/91234567",
            "add r/student n/Jamie Tan l/P1 pp/91234567",
            "list r/student", "enrol 1 lid/L1", "enrol 2 lid/L1",
            "edit 1 n/Alex Lee l/P3",
            "editlesson lid/L1 st/1630 s/Advanced Math rm/R3",
            "mark 1 lid/L1 d/2026-10-05 s/present",
            "mark 2 lid/L1 d/2026-10-05 s/absent",
        ]:
            session.command(command)
        unchanged = data.read_bytes()
        session.reject("edit 1 l/P7")
        session.reject("edit 1 n/Jamie Tan pp/91234567")
        session.reject("editlesson lid/L2 st/1700")
        require(data.read_bytes() == unchanged, "Rejected edits changed operational data")
        session.command("edit 1 n/ Alex   Lee l/p3", "No changes to student: Alex Lee (S1).")
        require(data.read_bytes() == unchanged, "No-op person edit rewrote operational data")
        verify_state(data, members=("S1", "S2"), first_status="present")
        session.command("showlesson lid/L1 d/2026-10-05", "Showing lesson L1 for 2026-10-05.")
        session.screenshot("shared-attendance")
        session.command("mark 1 lid/L1 d/2026-10-05 s/absent")
        verify_state(data, members=("S1", "S2"))
        session.command("unenrol 1 lid/L1")
        session.command("history 1 lid/L1", "Showing 1 attendance record(s).")
        verify_state(data)
        original = data.read_bytes()
        session.screenshot("retained-history")
        session.exit()
        require(data.read_bytes() == original, "Shutdown rewrote operational data")
        session = PackageSession(folder, evidence, "restart", transcript, args.expected_version)
        session.ready()
        for command, feedback in [
            ("list r/student", "Showing 2 student(s)."),
            ("lessons", "Showing 2 lesson(s)."),
            ("lessons si/1", "Showing 0 lesson(s)."),
            ("lessons si/2", "Showing 1 lesson(s)."),
            ("showlesson lid/L1 d/2026-10-05", "Showing lesson L1 for 2026-10-05."),
            ("history 1 lid/L1", "Showing 1 attendance record(s)."),
        ]:
            session.command(command, feedback)
        for index in (1, 2):
            session.command(f"mark {index} lid/L1 d/2026-10-05 s/absent",
                            f"Attendance for Student S{index} in lesson L1 on 2026-10-05 "
                            "is already marked absent. No changes made.")
        verify_state(data)
        session.screenshot("restarted-history")
        session.exit()
        require(data.read_bytes() == original, "Restart/read-only commands rewrote operational data")
        result["passed"] = True
        print("PASS: exact JAR metadata/launch, edits, shared Lesson, attendance, history and restart")
    except Exception as failure:
        result["error"] = str(failure)
        raise
    finally:
        if session is not None:
            session.cleanup()
        (evidence / "result.json").write_text(json.dumps(result, indent=2) + "\n")
        (evidence / "commands.json").write_text(json.dumps(transcript, indent=2) + "\n")


if __name__ == "__main__":
    main()
