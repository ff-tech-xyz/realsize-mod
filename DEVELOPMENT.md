# Developing RealSize

[Player guide](README.md) · [Configuration](CONFIGURATION.md) ·
[Architecture](ARCHITECTURE.md)

This page covers building and verification, not installation for players.
Implementation evidence and remaining release gates are recorded separately in
[FIXES.md](FIXES.md). Present-tense product documentation is not release approval
or evidence that an acceptance check has been performed.

The rebuild is called v2. The artifact version is still controlled by
`mod_version` in `gradle.properties` and currently remains `1.2.0`; this docs
update does not change the JAR version or publish a release.

## Building and testing

One root Gradle project uses Mojang names, the non-remapping
`net.fabricmc.fabric-loom` plugin, and Java 25. Build pins live in
`gradle.properties`; there are no mapping JARs or legacy platform modules.

```bash
./gradlew clean build --console=plain
python3 scripts/server-smoke.py
./gradlew runClientGameTest --console=plain
```

In the PyreHaven checkout layout, the completed JAR is
`../../runtime/realsize-mod/build/libs/realsize-<version>.jar`.
The first command compiles and packages the JAR and runs nonempty JUnit and
Minecraft server suites. The smoke command boots that packaged JAR with Loom's
pinned game/dependency classpath, verifies dedicated-server client isolation,
then saves and stops. It accepts the development EULA only in its disposable
loopback runtime. `./gradlew runServer --console=plain` remains available for
an ordinary interactive development server.

The separate client command loads the same packaged JAR, not the production
source-output directories. It starts a real graphical client and exercises
single-player and a loopback dedicated connection, local option reload and
vanilla tracking limits. Linux without a display uses
`xvfb-run -a ./gradlew runClientGameTest --console=plain`; Xvfb and the game's
OpenGL/audio libraries must be available. These are automated gameplay checks,
not an unrestricted mod-compatibility or manual-play certification.

CI runs those commands with SHA-pinned actions, Java 25, and the checksum-pinned
Gradle wrapper. It requires nonempty reports and packaged-class provenance,
checks that client/server probes didn't change the JAR, and retains that JAR
with its hash and test logs. It does not publish a release. Exact acceptance
gates, measurements, and actual completion status live in [FIXES.md](FIXES.md),
not inferred from an empty build or a workflow file's existence.

## Documentation layout

- `README.md`: what the mod does, gameplay, installation and common questions.
- `CONFIGURATION.md`: server rules, client options, migration and default sizes.
- `DEVELOPMENT.md`: build commands, test scope and contributor navigation.
- `ARCHITECTURE.md`: implementation ownership and internal contracts.
- `FIXES.md`: recorded evidence and uncompleted acceptance gates.
- `LEGACY_README.md`: archived v1 guidance, not current installation instructions.
