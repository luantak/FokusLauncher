# Agents

## Cursor Cloud specific instructions

### Product

Fokus Launcher is a single-module Android launcher app (Kotlin + Jetpack Compose). There are no backend services, databases, or Docker containers to run — it is a purely client-side Android application.

### Environment

- **JDK 21** is required. `JAVA_HOME` must point to the JDK 21 installation (on Cloud VMs: `/usr/lib/jvm/java-21-openjdk-amd64`).
- **Android SDK** must be installed at `$ANDROID_HOME` (`/home/ubuntu/android-sdk`) with packages: `platform-tools`, `platforms;android-36`, `build-tools;36.0.0`.
- Both `JAVA_HOME` and `ANDROID_HOME`/`ANDROID_SDK_ROOT` are persisted in `~/.bashrc`.

### Key commands

See `README.md` § "Build From Source" and `.github/copilot-instructions.md` for full details. Quick reference:

| Task | Command |
|------|---------|
| Build debug APK | `./gradlew :app:assembleDebug` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Run lint | `./gradlew lint` |
| Run specific test | `./gradlew test --tests "com.lu4p.fokuslauncher.ui.home.HomeViewModelTest"` |

### Caveats

- **Gradle Daemon JVM**: `gradle/gradle-daemon-jvm.properties` specifies `toolchainVendor=JETBRAINS`. The Gradle wrapper auto-provisions a JetBrains JDK for the daemon on first run if one isn't present. This is expected and happens transparently.
- **Lint pre-existing errors**: `./gradlew lint` reports ~91 `NewApi` errors that exist on `master`. These are pre-existing and not caused by agent changes. The CI workflow does **not** run lint — it only runs `testDebugUnitTest` and `assembleDebug`.
- **No emulator/device needed for unit tests**: `testDebugUnitTest` uses Robolectric, so tests run on the JVM without an Android device or emulator.
- **Configuration cache**: Gradle configuration cache is enabled (`gradle.properties`). Subsequent builds are significantly faster after the first run.
