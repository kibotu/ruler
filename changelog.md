# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.1.0] - 2025-11-26

### Added

- Size verification with configurable thresholds for download and install size
- Configuration cache support via `@CacheableTask`
- DexGuard and ProGuard mapping file support
- Published to Gradle Plugin Portal
- Published to Maven Central

### Changed

- Migrated from `kotlin-js` to `kotlin-multiplatform` plugin
- Gradle 8.4 → 9.2.0
- Android Gradle Plugin 8.2.0 → 8.13.1
- Kotlin 1.9.10 → 2.2.21
- Kotlin React wrappers updated to 2025.11.11
- Publishing now uses fat JAR (single dependency)

### Fixed

- Kotlin/JS API migrations (jso, Fragment.create, useEffect)
- Clikt 5.0 API compatibility
- DexBackedDexFile API changes
- Insights page rendering issues

## [2.0.0-beta-3]

### Added

- Added support for Android Gradle Plugin 7.4.x

## [2.0.0-alpha-2] - 2023-03-31

### Added

- Published ruler-cli JAR as a separate artifact

## [2.0.0-alpha-1] - 2023-03-31

### Added

- Added ruler-cli to allow usage of Ruler from non-Gradle build systems

### Changed

- Extracted non-Gradle specific code to ruler-common

