# Repository Guidelines

## Project Structure & Module Organization

This repository is an IntelliJ IDEA plugin for exporting selected project files to a JAR. Main plugin code lives under `src/main/java/org/yanhuang/plugins/intellij/exportjar`, with supporting Kotlin code in `src/main/kotlin`. Plugin registration and actions are declared in `src/main/resources/META-INF/plugin.xml`. Tests are in `src/test/java` and `src/test/kotlin`, with fixtures/resources under `src/test/resources`. Documentation assets and screenshots are stored in `image/`; release notes are maintained in `CHANGELOG.md`.

## Build, Test, and Development Commands

Use the Gradle wrapper rather than a system Gradle install.

- `.\gradlew.bat test` / `./gradlew test`: run unit and platform tests.
- `.\gradlew.bat buildPlugin` / `./gradlew buildPlugin`: build the plugin ZIP under `build/distributions`.
- `.\gradlew.bat clean buildPlugin verifyPlugin -i -V`: clean, package, and verify against IDE versions listed in `gradle.properties`.
- `.\gradlew.bat runIde` / `./gradlew runIde`: launch a sandbox IDE with the plugin installed for manual testing.

The project targets Java bytecode level 11 and uses IntelliJ Platform Gradle Plugin settings from `gradle.properties`.

## Coding Style & Naming Conventions

Follow the existing Java/Kotlin style: 4-space indentation, braces on the same line, package names under `org.yanhuang.plugins.intellij.exportjar`, and descriptive class names that match IntelliJ concepts, such as `ExportJarAction`, `SettingDialog`, and `CommonUtilsTest`. Keep UI code in `ui`, VCS/local-changes integration in `changes`, settings persistence in `settings`, and shared helpers in `utils`. Prefer IntelliJ Platform APIs for threading, VFS, PSI, dialogs, and actions.

## Testing Guidelines

Tests use JUnit 4 and IntelliJ test fixtures such as `BasePlatformTestCase`. Name Java tests `*Test.java` and Kotlin tests `*Test.kt`; name methods after the behavior under test, for example `testCollectFilesNest`. Add focused tests for utility logic and regressions around file collection, VFS behavior, and export options. Run `.\gradlew.bat test` before submitting changes.

## Commit & Pull Request Guidelines

Recent commits use short imperative summaries, often lowercase, such as `update CHANGELOG.md` or `make compatible with before 2022.1`. Keep the first line concise and specific. Pull requests should describe the behavior change, list manual or Gradle verification performed, link related issues, and include screenshots or GIFs for UI/dialog changes.

## Security & Configuration Tips

Do not commit signing credentials or publish tokens. `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, and `PUBLISH_TOKEN` are read from environment variables. Keep generated outputs such as `build/`, `out/`, `.gradle/`, and local IDE metadata out of reviews unless intentionally updating project configuration.
