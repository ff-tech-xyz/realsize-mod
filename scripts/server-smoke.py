#!/usr/bin/env python3
"""Boot the real dev server, check client isolation, and stop it cleanly."""

import os
from pathlib import Path
import signal
import subprocess
import time


def main():
    project = Path(__file__).resolve().parents[1]
    runtime = project.parent.parent / "runtime/realsize-mod"
    run = runtime / "run/server-smoke"
    evidence = runtime / "evidence"
    run.mkdir(parents=True, exist_ok=True)
    evidence.mkdir(parents=True, exist_ok=True)
    log = evidence / "server-smoke.log"
    classes = evidence / "server-classes.log"
    classes.unlink(missing_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25586\nonline-mode=true\n"
        "level-name=scaffold-smoke\nview-distance=2\nsimulation-distance=2\n"
        "max-players=1\n")
    init = evidence / "server-smoke.gradle"
    init.write_text(f"""gradle.projectsEvaluated {{
    rootProject.loom.runs.server {{
        runDirectory.set(file('{run}'))
        vmArg '-Xlog:class+load=info:file={classes}'
    }}
    rootProject.tasks.named('runServer') {{
        standardInput = System.in
    }}
}}
""")
    command = [str(project / "gradlew"), "--no-daemon", "-I", str(init),
               "runServer", "--console=plain"]
    with log.open("w") as output:
        process = subprocess.Popen(command, cwd=project, stdin=subprocess.PIPE,
                                   stdout=output, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        assert process.stdin is not None
        stopped = False
        try:
            deadline = time.monotonic() + 180
            while process.poll() is None and time.monotonic() < deadline:
                text = log.read_text()
                if not stopped and ')! For help, type "help"' in text:
                    process.stdin.write(b"stop\n")
                    process.stdin.flush()
                    stopped = True
                time.sleep(0.25)
            if process.poll() is None:
                raise RuntimeError(f"Server did not finish within 180 seconds; see {log}")
            if process.returncode != 0 or not stopped:
                raise RuntimeError(f"Server did not reach startup and clean shutdown; see {log}")
        finally:
            process.stdin.close()
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()
    text = log.read_text()
    loaded = classes.read_text()
    assert "RealSize scaffold initialized; gameplay features are not implemented yet." in text
    assert "xyz.pyrehaven.realsize.RealSizeMod source:" in loaded
    assert "xyz.pyrehaven.realsize.client." not in loaded
    assert "net.minecraft.client." not in loaded
    assert "RealSize client scaffold initialized" not in text
    assert "Saving worlds" in text
    print("PASS: dedicated server initialized RealSize, reached Done, saved worlds, and exited 0")
    print("PASS: JVM class-load trace contains RealSizeMod and no RealSize or Minecraft client classes")
    print(f"Logs: {log} and {classes}")


if __name__ == "__main__":
    main()
