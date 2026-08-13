# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.1.0]

### Changed
- The HTML report renders each module's file list when that module is expanded, instead of rendering
  every file of every module up front.
- Attribution indexes the dependency graph by package and by class name, instead of scanning it once
  per unresolved file.
  
### Fixed

- The report paths are printed on every build. They used to be logged from the analysis itself, so an
  up-to-date or cached run said nothing about where the reports were. A `printRuler<Variant>Reports`
  finalizer task now reports them, and the analysis stays cacheable.
- Kotlin standard library classes that R8 synthesizes are attributed to `org.jetbrains.kotlin:kotlin-stdlib`
  again. A synthetic `kotlin` component made that package ambiguous, which sent them to the application
  module instead.
- Indented lines in a DexGuard resource mapping file are read, so those resource names are de-obfuscated.
- The publish workflow triggers on `v`-prefixed tags, which is how this project tags releases, and strips
  the prefix from the published version.

### Removed

- The second JSON payload of pre-computed insights in `report.html`. The page read one figure from it
  and computed the rest itself, so the payload only made the report larger.

## [3.0.0]

### Added

- Self-contained HTML report with treemap, top-20 file lists, and per-owner totals in one offline file.
- `previewReport` task to open the HTML report from a fixture or custom JSON file.
- Functional tests with Gradle TestKit and configuration-cache coverage.
- Shadow JAR with relocated `kotlinx-serialization` and SnakeYAML to avoid classpath clashes with other plugins.
- Version catalog (`gradle/libs.versions.toml`) for dependency management.
- Publish workflow on version tags to Maven Central and the Gradle Plugin Portal.
- Size analysis feature parity with Caliper.

### Changed

- Rebuilt the project as a single `ruler` module under `com.kibotu.ruler`, replacing the multi-module layout.
- Android Gradle Plugin 8.13.1 → 9.3.1, Kotlin 2.2.21 → 2.4.10, Gradle 9.2 → 9.7.
- Requires JDK 17+.
- Removed `buildSrc`; shared versions now live in the version catalog.
- Sample app simplified; dynamic feature module removed.

### Removed

- `ruler-cli` standalone CLI artifact.
- `ruler-frontend` Kotlin/JS React UI and its Selenium-based test module.
- `ruler-e2e-tests` module.
- Separate `ruler-common` and `ruler-models` published artifacts; analysis code ships inside the plugin JAR.
- Legacy documentation screenshots.

## [2.1.12] - 2025-11-26

## [2.1.11] - 2025-11-26

### Changed

- README updates.

## [2.1.10] - 2025-11-26

### Changed

- CI workflow ignores tag pushes.

## [2.1.9] - 2025-11-26

## [2.1.8] - 2025-11-26

### Changed

- Publishing pipeline adjustments.

## [2.1.7] - 2025-11-26

## [2.1.6] - 2025-11-26

## [2.1.5] - 2025-11-26

## [2.1.4] - 2025-11-26

### Changed

- Publishing pipeline adjustments.

## [2.1.3] - 2025-11-26

## [2.1.2] - 2025-11-26

## [2.1.1] - 2025-11-26

### Added

- Maven Central publish job in the release workflow.

### Changed

- Gradle configuration-cache support in frontend integration tests.
- Sonatype Central Portal URLs for Maven Central publishing.
- Version override via `-Pversion` for release builds.
- Conditional artifact signing when signing credentials are absent.
- Javadoc configuration for published library modules.
- POM metadata points to `kibotu/ruler` and lists the maintainer.

## [2.1.0] - 2025-11-26

### Added

- Size verification with configurable thresholds for download and install size.
- Configuration cache support via `@CacheableTask`.
- DexGuard and ProGuard mapping file support.
- Published to Gradle Plugin Portal.
- Published to Maven Central.

### Changed

- Migrated from `kotlin-js` to `kotlin-multiplatform` plugin.
- Gradle 8.4 → 9.2.0.
- Android Gradle Plugin 8.2.0 → 8.13.1.
- Kotlin 1.9.10 → 2.2.21.
- Kotlin React wrappers updated to 2025.11.11.
- Publishing now uses fat JAR (single dependency).

### Fixed

- Kotlin/JS API migrations (`jso`, `Fragment.create`, `useEffect`).
- Clikt 5.0 API compatibility.
- DexBackedDexFile API changes.
- Insights page rendering issues.

## [2.0.0-beta-3]

### Added

- Support for Android Gradle Plugin 7.4.x.

## [2.0.0-alpha-2] - 2023-03-31

### Added

- Published `ruler-cli` JAR as a separate artifact.

## [2.0.0-alpha-1] - 2023-03-31

### Added

- `ruler-cli` to allow usage of Ruler from non-Gradle build systems.

### Changed

- Extracted non-Gradle specific code to `ruler-common`.

[unreleased]: https://github.com/kibotu/ruler/compare/3.1.0...HEAD
[3.1.0]: https://github.com/kibotu/ruler/compare/3.0.0...3.1.0
[3.0.0]: https://github.com/kibotu/ruler/compare/2.1.12...3.0.0
[2.1.12]: https://github.com/kibotu/ruler/compare/2.1.11...2.1.12
[2.1.11]: https://github.com/kibotu/ruler/compare/2.1.10...2.1.11
[2.1.10]: https://github.com/kibotu/ruler/compare/2.1.9...2.1.10
[2.1.9]: https://github.com/kibotu/ruler/compare/2.1.8...2.1.9
[2.1.8]: https://github.com/kibotu/ruler/compare/2.1.7...2.1.8
[2.1.7]: https://github.com/kibotu/ruler/compare/2.1.6...2.1.7
[2.1.6]: https://github.com/kibotu/ruler/compare/2.1.5...2.1.6
[2.1.5]: https://github.com/kibotu/ruler/compare/2.1.4...2.1.5
[2.1.4]: https://github.com/kibotu/ruler/compare/2.1.3...2.1.4
[2.1.3]: https://github.com/kibotu/ruler/compare/2.1.2...2.1.3
[2.1.2]: https://github.com/kibotu/ruler/compare/2.1.1...2.1.2
[2.1.1]: https://github.com/kibotu/ruler/compare/2.1.0...2.1.1
[2.1.0]: https://github.com/kibotu/ruler/compare/2.0.0-beta-3...2.1.0
[2.0.0-beta-3]: https://github.com/kibotu/ruler/compare/2.0.0-alpha-2...2.0.0-beta-3
[2.0.0-alpha-2]: https://github.com/kibotu/ruler/compare/2.0.0-alpha-1...2.0.0-alpha-2
[2.0.0-alpha-1]: https://github.com/kibotu/ruler/releases/tag/2.0.0-alpha-1
