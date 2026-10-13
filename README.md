# RealSize 1.2.0 rebuild

This branch is build-tool groundwork, not an installable mod. The legacy
implementation has been removed; no replacement scaffold exists yet.

The proposed server-side Minecraft 26.3 design, ownership boundaries,
configuration compatibility, and implementation phases are in
[ARCHITECTURE.md](ARCHITECTURE.md). Implementation waits for phase approval.

The original product and configuration documentation is preserved verbatim in
[LEGACY_README.md](LEGACY_README.md). Its old build commands, module names, and
compatibility claims do not describe this branch. The approved icon is retained
at `branding/icon.png`, and the license is unchanged.

## Build-tool setup

Versions checked against live official metadata on October 13, 2026:

- Minecraft: `26.3`
- Fabric Loom: `1.18.4`, latest non-prerelease release
- Fabric Loader: `0.19.5`, latest stable for 26.3
- Fabric API: `0.162.0+26.3`
- Gradle: `9.8.1`, with distribution SHA-256 pinned in the wrapper properties
- Java toolchain and compiler release: `25`

The root project uses `net.fabricmc.fabric-loom` for unobfuscated Minecraft.
There are no Yarn mappings, local identity-mapping JARs, or legacy subprojects.
The full Fabric API pin establishes the development baseline; narrow runtime
module requirements will be set when the real entrypoint is implemented.

From this checkout, with JDK 25 installed:

```bash
./gradlew --version
./gradlew clean build dependencies --configuration runtimeClasspath --console=plain
```

Both commands were run successfully for this groundwork. Loom configured at
`1.18.4`; the dependency report resolved Minecraft 26.3, Loader 0.19.5, and Fabric
API 0.162.0+26.3 without failed dependencies. Compilation and tests reported
`NO-SOURCE`; the JAR task is intentionally disabled. This proves tooling and
dependency resolution, not gameplay or server compatibility.

Generated build output goes to `../../runtime/realsize-mod/build` in the
PyreHaven workspace layout. The wrapper distribution and wrapper JAR checksums
were verified against Gradle's published values. No tests or server session are
claimed, and no release has been produced.
