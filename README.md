# WIP
you have stumbled upon the compose version of echo

## Project structure
- `client`: shared Echo logic, Compose UI, resources, and platform-specific implementations.

The app itself is multiple Gradle application modules that depend on the `client` module.
- `apps/android`
- `apps/desktop` (Linux, Windows, Mac) (we don't allow JVM here)
- `apps/web`
- `apps/ios`

To run:
- Android: `./gradlew :apps:android:runDebug`
- Desktop: `./gradlew :apps:desktop:runDebugExecutableDesktop` (Windows: `gradlew.bat :apps:desktop:runDebugExecutableDesktop`)
- Web: `./gradlew :apps:web:wasmJsBrowserDevelopmentRun`
- iOS: Open `apps/ios/iosApp.xcodeproj` in Xcode
