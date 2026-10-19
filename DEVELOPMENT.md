# Developing RealSize

[README](README.md) · [Configuration](CONFIGURATION.md) · [Architecture](ARCHITECTURE.md)

## Build and test

Use Java 25 and the Gradle wrapper. Dependency versions are in `gradle.properties`.

```bash
./gradlew clean build --console=plain
python3 scripts/server-smoke.py
./gradlew runClientGameTest --console=plain
```

In the PyreHaven checkout, JARs are written to
`../../runtime/realsize-mod/build/libs/`.

- `build` packages the mod and runs JUnit and Minecraft server tests.
- `server-smoke.py` boots the packaged JAR on a disposable loopback server,
  checks client-code isolation, then saves and stops. It accepts the EULA for
  that test instance.
- `runClientGameTest` tests the packaged JAR in a graphical client, covering
  single-player, a loopback dedicated connection, local option reload and
  tracking limits.

On Linux without a display, use
`xvfb-run -a ./gradlew runClientGameTest --console=plain`. Xvfb and the game's
OpenGL/audio libraries must be installed. For an interactive development server,
use `./gradlew runServer --console=plain`.

CI runs these checks with Java 25, requires nonempty test reports, verifies that
the gameplay probes use the packaged JAR without changing it, and retains the
JAR, hash and logs as artifacts.

## Project references

- [Architecture](ARCHITECTURE.md): code ownership and implementation contracts.
- [Verification tracker](FIXES.md): test evidence and outstanding checks.
- [Legacy README](LEGACY_README.md): archived v1 documentation.
