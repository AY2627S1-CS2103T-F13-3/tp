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


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def run(*arguments, **options):
    return subprocess.run(arguments, check=True, text=True, timeout=15, **options)


class PackageSession:
    def __init__(self, folder, evidence, name, transcript):
        self.folder = folder
        self.evidence = evidence
        self.name = name
        self.transcript = transcript
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
        # The stage is mapped before UiManager finishes filling its controls.
        time.sleep(0.5)
        run("xdotool", "windowfocus", "--sync", self.window)
        # CommandBox is directly below the menu, in client-window coordinates.
        run("xdotool", "mousemove", "--window", self.window, "200", "45", "click", "1")

    def command(self, text):
        offset = self.log.stat().st_size
        run("xdotool", "key", "--clearmodifiers", "ctrl+a")
        run("xdotool", "type", "--clearmodifiers", "--delay", "1", text)
        run("xdotool", "key", "Return")

        def completed():
            with self.log.open() as log:
                log.seek(offset)
                output = log.read()
            require("An error occurred while executing command:" not in output,
                    f"Command rejected: {text}\n{output}")
            require("SEVERE:" not in output, f"Fatal application error: {output}")
            return output if "Result: " in output else None

        output = self.wait(completed, f"successful command {text!r}")
        self.transcript.append({"session": self.name, "command": text, "output": output})
        time.sleep(0.2)  # Allow the resulting control updates to reach the next display pulse.

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


def verify_state(path):
    state = json.loads(path.read_text())
    require([person["id"] for person in state["people"]] == ["T1", "S1", "S2"],
            "Creation order or stable person IDs differ")
    require(state["personCounters"] == {"STUDENT": 2, "TUTOR": 1, "PARENT": 0},
            "Person allocation counters differ")
    require(len(state["lessons"]) == 1, "Expected one shared Lesson")
    lesson = state["lessons"][0]
    require(lesson["id"] == "L1" and lesson["tutorId"] == "T1"
            and lesson["enrolledStudentIds"] == ["S2"], "Shared Lesson membership differs")
    records = {(record["studentId"], record["lessonId"], record["date"], record["status"])
               for record in state["attendance"]}
    require(len(state["attendance"]) == 2 and records == {
        ("S1", "L1", "2026-10-05", "absent"),
        ("S2", "L1", "2026-10-05", "absent"),
    }, "Independent/corrected/former-member attendance differs")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", type=Path, required=True)
    parser.add_argument("--source", required=True)
    parser.add_argument("--expected-sha256", required=True)
    parser.add_argument("--evidence", type=Path, required=True)
    args = parser.parse_args()
    evidence = args.evidence.resolve()
    evidence.mkdir(parents=True, exist_ok=True)
    transcript = []
    session = None
    result = {"passed": False, "source": args.source, "platform": platform.platform(),
              "architecture": platform.machine(), "display": os.environ.get("DISPLAY"),
              "scope": "Actual packaged-JAR X11 automation; no manual physical-display check"}
    try:
        content = args.jar.read_bytes()
        digest = hashlib.sha256(content).hexdigest()
        result.update(jar_sha256=digest, jar_bytes=len(content))
        require(digest == args.expected_sha256.lower(), "Candidate JAR SHA-256 mismatch")
        with (evidence / "runtime.txt").open("w") as runtime:
            run("java", "-XshowSettings:properties", "-version",
                stdout=runtime, stderr=subprocess.STDOUT)
        folder = evidence / "fresh application folder"
        folder.mkdir()
        shutil.copyfile(args.jar, folder / "ponhub.jar")
        data = folder / "data" / "ponhub.json"
        session = PackageSession(folder, evidence, "initial", transcript)
        session.ready()
        for command in [
            "add r/tutor n/Mei Lim p/92345678 e/mei@example.com",
            "addlesson d/Mon st/1600 et/1730 s/Math tu/Mei Lim rm/R1",
            "add r/student n/Alex Tan l/S2 pp/91234567",
            "add r/student n/Jamie Tan l/P1 pp/91234567",
            "list r/student", "enrol 1 lid/L1", "enrol 2 lid/L1",
            "mark 1 lid/L1 d/2026-10-05 s/present",
            "mark 2 lid/L1 d/2026-10-05 s/absent",
            "showlesson lid/L1 d/2026-10-05",
        ]:
            session.command(command)
        session.screenshot("shared-attendance")
        for command in ["mark 1 lid/L1 d/2026-10-05 s/absent", "unenrol 1 lid/L1",
                        "history 1 lid/L1"]:
            session.command(command)
        verify_state(data)
        original = data.read_bytes()
        session.screenshot("retained-history")
        session.exit()
        require(data.read_bytes() == original, "Shutdown rewrote operational data")
        session = PackageSession(folder, evidence, "restart", transcript)
        session.ready()
        for command in ["list r/student", "lessons", "showlesson lid/L1 d/2026-10-05",
                        "history 1 lid/L1"]:
            session.command(command)
        verify_state(data)
        session.screenshot("restarted-history")
        session.exit()
        require(data.read_bytes() == original, "Restart/read-only commands rewrote operational data")
        result["passed"] = True
        print("PASS: exact JAR launch, shared Lesson, attendance correction, retained history and restart")
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
