#!/usr/bin/env python3
"""Boot the packaged JAR on pinned Loom dependencies and verify server isolation."""

import os
import hashlib
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
    jar = runtime / 'build/libs/realsize-1.2.0.jar'
    if not jar.is_file():
        raise RuntimeError('Run ./gradlew build before the packaged server smoke test')
    log = evidence / "server-smoke.log"
    classes = evidence / "server-classes.log"
    classes.unlink(missing_ok=True)
    (run / "eula.txt").write_text("eula=true\n")
    (run / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25586\nonline-mode=true\n"
        "level-name=scaffold-smoke\nview-distance=2\nsimulation-distance=2\n"
        "max-players=1\n")
    init = evidence / "server-smoke.gradle"
    classpath_file = evidence / 'server-classpath.txt'
    init.write_text(f"""gradle.projectsEvaluated {{
    rootProject.tasks.register('smokeClasspath') {{
        doLast {{ new File('{classpath_file}').text = rootProject.tasks.named('runServer').get().classpath.asPath }}
    }}
}}
""")
    subprocess.run([str(project / 'gradlew'), '-I', str(init), 'smokeClasspath',
                    '--console=plain'], cwd=project, check=True)
    classpath = [p for p in classpath_file.read_text().split(os.pathsep)
                 if not Path(p).is_relative_to(runtime / 'build/classes')
                 and not Path(p).is_relative_to(runtime / 'build/resources')]
    common, = [p for p in classpath if Path(p).name.startswith('minecraft-common-')]
    classpath.append(str(jar))
    command = ['java', '-Xmx1G', f'-Xlog:class+load=info:file={classes}',
               '-Dfabric.development=true', f'-Dfabric.gameJarPath={common}',
               '-cp', os.pathsep.join(classpath),
               'net.fabricmc.loader.impl.launch.knot.KnotServer', 'nogui']
    with log.open("w") as output:
        process = subprocess.Popen(command, cwd=run, stdin=subprocess.PIPE,
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
    assert "RealSize initialized; transient entity sizing lifecycle registered." in text
    assert "xyz.pyrehaven.realsize.RealSizeMod source:" in loaded
    assert "xyz.pyrehaven.realsize.sizing.EntitySizing source:" in loaded
    own_classes = [line for line in loaded.splitlines()
                   if 'xyz.pyrehaven.realsize.' in line and ' source:' in line]
    assert any(f'RealSizeMod source: file:{jar}' in line for line in own_classes)
    assert all('build/classes/' not in line for line in own_classes)
    assert 'realsize-test' not in text
    assert "xyz.pyrehaven.realsize.client." not in loaded
    assert "net.minecraft.client." not in loaded
    assert "RealSize client scaffold initialized" not in text
    assert "Saving worlds" in text
    print("PASS: dedicated server initialized RealSize, reached Done, saved worlds, and exited 0")
    print("PASS: JVM class-load trace contains RealSizeMod and no RealSize or Minecraft client classes")
    print(f"Logs: {log} and {classes}")
    print(f"Packaged artifact: {jar}; SHA256 {hashlib.sha256(jar.read_bytes()).hexdigest()}")


if __name__ == "__main__":
    main()
