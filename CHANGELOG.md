# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [3.3.1]

### Changed

- Comments were pruned across the sources, from 254 lines to 145. Those that restated the code
  or narrated history git already keeps were removed; those that explain why the code is the
  way it is were kept and tightened. No behaviour change.

## [3.3.0]

### Fixed

- The HTML treemap scored a candidate row of cells against the region's pixel area, which
  left the side length it minimised unrelated to the side the row was laid out on. Cells
  came out as slivers rather than blocks: on a 430-component app, a p90 aspect ratio of
  17:1 and 45% of cells worse than 3:1, against 1.5 and none for the same data laid out by
  d3. The row side is now normalised by the remaining value, which makes the layout agree
  with d3's treemap to within floating point while adding no dependency.
- The HTML treemap laid out groups that were only a little larger than the gutter they are
  inset by, handing their files a box smaller than that gutter and leaving sub-pixel cells.
  Combined with the slivers above, the chart rendered as a field of 1px stripes.
- The HTML treemap was drawn at 400px tall in a box up to 1340px wide, close to the worst
  shape a squarified treemap can be laid out in. It now matches the 600px the comparison
  report uses.
- The HTML treemap gave a cell's absolute size with nothing to compare it against. Tooltips
  now also carry that cell's share of the app, measured against what the treemap draws.
- The HTML treemap coloured each cell by its position in the layout, so every component's
  files came out an unrelated rainbow. Each component now has one hue and its files are
  tints of it, which reads as blocks of components with internal structure. Colouring by
  owner, as the comparison report does, was not an option: the app above has 104 distinct
  owners, so an ordinal scale would cycle eight times and mean nothing.
- The HTML treemap emitted its cells at zero size and relied on an animation to give them
  one, so reduced motion, scripting turned off, and headless captures all showed an empty
  chart. Cells are now drawn at their final size, and a timer restores that size if the
  animation frames are throttled or never arrive, so the entrance cannot strand the chart
  half-drawn.

### Added

- Tests pinning the treemap's layout invariants: that a candidate row is scored against the
  value left to lay out, that each cell is sized by install size against the region, that a
  group is not laid out inside its own padding, that cells are emitted at their final size,
  that the entrance restores that size when its frames never arrive, and that a tooltip
  carries the cell's share of the app. They assert on the template's script source, so they
  guard against the regressions above returning; they are not a substitute for checking the
  rendered page.

## [3.2.0]

### Fixed

- The HTML treemap picked which components and files to show by download size but laid the
  cells out by install size. The component at the top of the layout was therefore not
  necessarily the largest one, and which entries fell into the "other" bucket depended on
  the wrong metric. Both now use install size, the metric the cells are sized by.
- The ownership chart counted only each component's primary owner, while the drill-down
  below it counted every owner. The two views disagreed and the chart's per-owner totals
  summed to less than the app total. The chart now counts co-owned components under each
  owner, so the totals add up. Per-owner totals therefore exceed the app total by the size
  of each co-owned component: on the debug sample, 555,601 bytes across two co-owned
  components; on the release sample, 123,863 bytes.
- The ownership drill-down and the dynamic feature list read the Breakdown tab's sort
  setting, so the Ownership tab's own sort control had no effect there. A dynamic feature
  also showed install size in its header while listing its files by download size. Module
  cards now take the size key explicitly, defaulting to the Breakdown tab.
- CI failed in `setup-android` before the build started. The action defaults to installing
  `tools platform-tools`, and Google has removed the legacy `tools` package from the SDK
  repository, so `sdkmanager` exits 1. CI now requests only the packages the build needs.
  This affected every push since the last green run in August, not just this branch.

### Added

- Tests pinning the report's treemap sort key, its ownership accounting, and its per-tab
  size keys. They assert on the template's script source, so they guard against the
  regressions above returning; they are not a substitute for checking the rendered page.

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

[unreleased]: https://github.com/kibotu/ruler/compare/3.3.1...main
[3.3.1]: https://github.com/kibotu/ruler/compare/3.3.0...3.3.1
[3.3.0]: https://github.com/kibotu/ruler/compare/3.2.0...3.3.0
[3.2.0]: https://github.com/kibotu/ruler/compare/3.1.1...3.2.0
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
