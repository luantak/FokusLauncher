# Fokus Launcher - Copilot Instructions

## Project Overview

Fokus Launcher is an Android launcher application designed for a minimal, clean user experience. The app is built with modern Android development practices using Kotlin, Jetpack Compose, and Material 3.

**Tech Stack:**
- Language: Kotlin
- UI Framework: Jetpack Compose with Material 3
- Dependency Injection: Hilt
- Database: Room
- Preferences: DataStore
- Navigation: Navigation Compose
- Minimum SDK: 26 (Android 8.0)
- Compile/Target SDK: 36
- Build Tool: Gradle 9.1.0 with AGP 9.0.0
- Required JDK: 21 (Temurin)

## Build & Test Instructions

### Environment Setup

**CRITICAL:** Always use JDK 21. The project requires JDK 21 as specified in the GitHub Actions workflow. The `gradle/gradle-daemon-jvm.properties` file specifies a JetBrains JDK vendor, but in environments where this is not available (like CI with Temurin), the build may require toolchain auto-provisioning or manual JDK configuration.

### Building the Project

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK (requires signing config)
./gradlew :app:assembleRelease

# Install debug APK on connected device
./gradlew installDebug
```

**Build Outputs:**
- Debug APK: `app/build/outputs/apk/debug/`
- Release APK: `app/build/outputs/apk/release/`

**Build Notes:**
- Clean builds are recommended when switching between branches or after major dependency changes
- The first build downloads Gradle wrapper and dependencies, which can take several minutes
- Release builds require signing configuration via environment variables or `gradle.properties`:
  - `ANDROID_KEYSTORE_PATH`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

### Running Tests

```bash
# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.lu4p.fokuslauncher.ui.home.HomeViewModelTest"
```

**Test Locations:**
- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/java/`

**Testing Framework:**
- JUnit 4 for test structure
- MockK for mocking
- Turbine for Flow testing
- Coroutines Test for suspend function testing
- Hilt for dependency injection in tests

### Linting & Code Quality

```bash
# Run lint checks
./gradlew lint

# Format code (if ktlint is configured)
./gradlew ktlintFormat
```

**Note:** The project uses Kotlin official code style as specified in `gradle.properties`.

## Project Structure

### Source Code Layout

```
app/src/main/java/com/lu4p/fokuslauncher/
├── FokusLauncherApp.kt          # Application class with Hilt setup
├── MainActivity.kt               # Single activity with Compose setup
├── data/                        # Data layer
│   ├── database/                # Room database entities and DAOs
│   ├── local/                   # Local data sources (PreferencesManager)
│   ├── model/                   # Data models
│   └── repository/              # Repository implementations
├── di/                          # Hilt dependency injection modules
├── ui/                          # UI layer (Compose screens & ViewModels)
│   ├── components/              # Reusable Compose components
│   ├── drawer/                  # App drawer screen
│   ├── home/                    # Home screen
│   ├── navigation/              # Navigation graph
│   ├── onboarding/              # First-run onboarding
│   ├── settings/                # Settings screen
│   └── theme/                   # Material 3 theme configuration
└── utils/                       # Utility classes
```

### Configuration Files

- `build.gradle.kts` (root): Top-level build configuration
- `app/build.gradle.kts`: App module build configuration with dependency definitions
- `gradle/libs.versions.toml`: Centralized dependency version catalog
- `gradle.properties`: Gradle and Android build properties
- `app/proguard-rules.pro`: ProGuard rules for release builds
- `app/src/main/AndroidManifest.xml`: App manifest with permissions and launcher configuration

### Key Architectural Patterns

1. **MVVM Architecture**: UI screens use ViewModels to manage state and business logic
2. **Repository Pattern**: Data layer uses repositories to abstract data sources
3. **Dependency Injection**: Hilt provides dependencies throughout the app
4. **Unidirectional Data Flow**: ViewModels expose StateFlows that UI observes
5. **Compose UI**: Declarative UI with Material 3 components

## Continuous Integration

The project uses GitHub Actions for CI/CD (`.github/workflows/android-build-and-release.yml`):

**Build Workflow (on push/PR):**
1. Checkout code
2. Set up JDK 21 (Temurin)
3. Set up Gradle with caching
4. Set up Android SDK (platform-tools, platforms;android-36, build-tools;36.0.0)
5. Build debug APK with `./gradlew :app:assembleDebug`
6. Upload APK as artifact

**Release Workflow (on GitHub release):**
1. Same setup as build workflow
2. Validate signing secrets are present
3. Decode keystore from base64
4. Build signed release APK with `./gradlew :app:assembleRelease`
5. Attach APK to GitHub release

**To ensure CI passes:**
- Always test builds locally before pushing
- Ensure unit tests pass with `./gradlew testDebugUnitTest`
- Do not commit changes that break the build or tests
- Release builds in CI require all signing secrets to be configured

## Common Development Tasks

### Adding a New Dependency

1. Add version to `gradle/libs.versions.toml` in `[versions]` section
2. Add library reference in `[libraries]` section
3. Add implementation in `app/build.gradle.kts` dependencies block
4. Sync Gradle and verify build succeeds

### Adding a New Screen

1. Create screen composable in appropriate `ui/` subdirectory
2. Create ViewModel extending `ViewModel` with `@HiltViewModel` annotation
3. Add navigation route to `ui/navigation/NavGraph.kt`
4. Update navigation calls from other screens
5. Write unit tests for ViewModel in `app/src/test/`

### Working with Room Database

1. Define entity in `data/database/`
2. Create DAO interface with query methods
3. Add DAO to `AppDatabase` class
4. Update database version if schema changes
5. Provide migration if needed
6. Access via repository pattern

### Modifying Dependencies

The project uses a version catalog (`gradle/libs.versions.toml`). When updating dependencies:
- Update version in `[versions]` section
- Changes propagate to all uses automatically
- Test thoroughly after dependency updates, especially major version changes

## Important Notes

### Android Permissions

The app uses several permissions documented in README.md:
- `QUERY_ALL_PACKAGES`: Enumerate installed apps
- `REQUEST_DELETE_PACKAGES`: Trigger uninstall flow
- `INTERNET`: Fetch weather data
- `ACCESS_COARSE_LOCATION`: Location-based weather
- `ACCESS_HIDDEN_PROFILES`: Android 15+ Private Space support
- `EXPAND_STATUS_BAR`: Expand notifications

When adding features that require new permissions, update both `AndroidManifest.xml` and README.md.

### Compose & Material 3

- Use Material 3 components from `androidx.compose.material3`
- Follow Material Design 3 guidelines for UI/UX
- Theme configuration is in `ui/theme/` directory
- Use existing composable components from `ui/components/` when possible

### State Management

- Use `StateFlow` for exposing state from ViewModels
- Collect state in Compose with `collectAsStateWithLifecycle()`
- Keep ViewModels free of Android framework dependencies (except lifecycle)
- Use Hilt to inject repositories and other dependencies into ViewModels

### Testing Guidelines

- Write unit tests for ViewModels with test coroutine dispatcher
- Mock repositories and data sources with MockK
- Test state flows with Turbine
- Keep tests focused on single functionality
- Follow existing test patterns in the codebase

## Troubleshooting

### Gradle Build Issues

- If toolchain download fails: The project specifies JetBrains JDK in `gradle/gradle-daemon-jvm.properties`. If not available, either enable toolchain auto-provisioning or use a compatible JDK 21 installation (Temurin is used in CI).
- If build is slow: Enable Gradle daemon and parallel execution in `gradle.properties`
- If dependencies fail to download: Check internet connection and Gradle cache
- If AGP version is not found: Verify that the Android Gradle Plugin version specified in `gradle/libs.versions.toml` exists and is available in Google's Maven repository

### Test Failures

- If tests time out: Increase timeout in test configuration
- If coroutine tests fail: Ensure test dispatcher is properly set up
- If Hilt tests fail: Verify `@HiltAndroidTest` annotation and test runner are configured

### Release Build Issues

- Release builds require signing configuration (see Build Instructions section)
- ProGuard may require additional rules for reflection-based libraries
- Test release builds locally before creating a release

## Best Practices

1. **Always run tests before committing**: Use `./gradlew testDebugUnitTest`
2. **Follow existing code patterns**: Maintain consistency with the current codebase
3. **Use Kotlin idioms**: Prefer `data class`, `sealed class`, extension functions, etc.
4. **Keep composables simple**: Extract complex logic to ViewModels
5. **Document complex logic**: Add comments for non-obvious implementations
6. **Handle edge cases**: Consider null safety, empty states, error states
7. **Use proper error handling**: Catch exceptions and provide user feedback
8. **Optimize for performance**: Use `remember`, `derivedStateOf`, keys appropriately in Compose
9. **Maintain backward compatibility**: Minimum SDK is 26, avoid APIs from newer versions without fallbacks
10. **Test on multiple Android versions**: Especially test launcher-specific functionality

## Additional Resources

- README.md: Project overview, features, and permissions
- LICENSE: GNU General Public License v3.0
- GitHub Issues: Track bugs and feature requests
- GitHub Actions: CI/CD pipeline configuration
