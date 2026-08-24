# krogu-time

`krogu-time` is a cleanroom Kotlin Multiplatform port of the Java 25 `java.time`
API, in the package `io.heapy.krogu.time`. It targets JVM, Android, and iOS.

The point is migration. A Java or Kotlin/JVM app that uses `java.time` can move
to Kotlin Multiplatform by changing the import, not the code. So the port keeps
the Java shape even where Kotlin would prefer another one: parameters that Java
accepts as `null` stay nullable here, and the arithmetic, the overflow checks,
and the exception messages match the JDK. Only APIs that do not exist outside
the JVM are replaced, such as `Stream`, which becomes `Sequence`.

The public Java 25 API surface is feature-complete. The date-time model,
chronology, formatting, temporal, and zone APIs are common Kotlin shared by every
target. JDK-specific integration, such as `java.text.Format`, is added on JVM and
Android only.

## Install

The library is published to Maven Central as `io.heapy:krogu-time`.

Kotlin Toolchain (`module.yaml`):

```yaml
dependencies:
  - io.heapy:krogu-time:0.1.0
```

Gradle (`build.gradle.kts`), from the `commonMain` source set:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.heapy:krogu-time:0.1.0")
        }
    }
}
```

Maven, for a JVM-only project:

```xml
<dependency>
    <groupId>io.heapy</groupId>
    <artifactId>krogu-time-jvm</artifactId>
    <version>0.1.0</version>
</dependency>
```

The published bytecode is class-file 65, so a Java 21 JVM can load it.

## Documentation

- [MIGRATION_NOTES.md](MIGRATION_NOTES.md) — the cases where converted Java code
  does not compile as-is, and what to write instead. Everything else is
  mechanical, and the IDE Java-to-Kotlin converter handles it.
- [ARCHITECTURE.md](ARCHITECTURE.md) — how the module is laid out, what is
  common code, and the four things each platform has to supply.
- [INTENT.md](INTENT.md) — the rules the port follows when a Java API and
  idiomatic Kotlin disagree.

## How java.time compliance is checked

Two checks run in CI on every push and pull request. A third runs monthly.

1. **Differential tests.** 97 test files in `test@jvm/` call `java.time` and
   `krogu-time` with the same input and assert the same result. They cover
   exhaustive and all-pairs matrices over standard fields, units, amounts,
   queries, adjustments, conversions, intervals, chronology factories, and
   chronology-aware ordering. They run on JDK 25, so JDK 25 is the reference.
2. **The same behavioral suite on every target.** The common tests in `test/`
   run on JVM, Android, and iOS. A behavior pinned against Java on the JVM is
   therefore also asserted on iOS, where no `java.time` exists to compare with.
3. **TZDB freshness.** A monthly workflow compares the bundled IANA database
   with the one the JDK ships and opens an issue when the JDK is ahead. Zone
   rules are the one input that goes stale on its own.

The current baseline is 709 JVM tests, 461 Android tests, and 460 iOS tests, all
passing, followed by a successful build of every target.

Run them:

```shell
./kotlin test
```

## Build

The repository uses JetBrains Kotlin Toolchain 0.12.0-dev-4300 and its checked-in
wrapper. No Gradle installation is required.

```shell
./kotlin test     # run every target's tests
./kotlin build    # build every target
```

The JDK is pinned to 25, because the JVM differential tests compare against the
`java.time` of the running JDK. The published bytecode is a separate knob and
stays at class-file 65, so the library still loads on a Java 21 JVM.

## Publishing

The module publishes to Maven Central as `io.heapy:krogu-time`. Publication
covers the JVM, Android, and iOS targets plus the shared Kotlin metadata.

`scripts/release.sh` does the whole release. It reads three variables:

```shell
export GPG_KEY_ID=...        # signing key, from gpg --list-secret-keys
export GPG_PASSPHRASE=...    # its passphrase
export CENTRAL_TOKEN=...     # Central Portal token, as "<username>:<password>"
scripts/release.sh
```

Keep those exports in a `publish.sh` wrapper at the repository root that calls
the script. That file is git-ignored, so the credentials stay local.

The script exports the signing key from the local GPG keyring at run time and
hands the toolchain what it wants: `KOTLIN_TOOLCHAIN_SIGNING_KEY`,
`KOTLIN_TOOLCHAIN_SIGNING_KEY_PASSPHRASE`,
`KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_USERNAME`, and
`KOTLIN_TOOLCHAIN_MAVEN_CENTRAL_PASSWORD`.

The publishing mode is `manual`. The upload is validated by Central and then
waits at https://central.sonatype.com/publishing/deployments until you release
it by hand.

## Compatibility contract

- Package root: `io.heapy.krogu.time`
- Behavioral reference: Java 25 `java.time`
- Bytecode target: class-file 65 (Java 21), so Java 21 JVMs can load the library
- API style: Kotlin properties and operators where they preserve Java
  semantics, with Java-named operations retained when useful
- Arithmetic: checked overflow and normalized values must match Java
- Development: red-green-refactor slices, with each green slice committed

## Coverage

Implemented public surface and foundations:

- `DateTimeException`
- `DateTimeParseException`
- KMP `Locale` values backed by canonical BCP 47 language tags, including
  Unicode locale-key lookup and platform FORMAT-locale discovery on JVM,
  Android, and iOS
- `ResolverStyle`, `FormatStyle`, `SignStyle`, and `TextStyle` formatter
  configuration enums
- `DecimalStyle` standard symbols, immutable symbol replacement, digit
  conversion, value semantics, Java-compatible text, available-locale lookup,
  and default/explicit locale factories honoring `nu` and `rg`, with immutable
  `DateTimeFormatter` overrides for localized numeric printing and parsing
- `DateTimeFormatter` core formatting, appendable output, parsing, temporal
  queries, ordered `parseBest` conversion, query-failure wrapping, and
  position-aware resolved and unresolved parsing through a KMP `ParsePosition`
  that maps to `java.text.ParsePosition` on JVM and Android, plus JVM/Android
  `toFormat` extensions providing Java-compatible `java.text.Format` adapters,
  with default and explicit locale factories plus immutable locale, resolver-style,
  resolver-field, chronology, and zone overrides, plus `localizedBy` calendar,
  numbering-system, region, and CLDR timezone-extension overrides,
  including pre-resolution field filtering and canonical chronology resolution
  across calendar, ordinal, proleptic-month, aligned-week, and ISO week dates,
  chronology-aware date conversion, instant conversion, parsed
  chronology, and parsed default-zone behavior, and strict/smart/lenient ISO,
  non-ISO calendar, and RFC 1123 resolution (including excess-day handling); numeric
  `ofPattern` date/time fields with Java-compatible width validation and
  descriptions, including ordinal day `D`, aligned week `F`,
  clock-hour `k`/`K`/`h`, milli/nano-of-day `A`/`N`, raw nano `n`, exact
  fractions, modified-Julian day `g`, numeric quarter `Q`/`q`, reduced and
  variable-width years,
  adjacent fields, quoted literals, ISO/RFC-style offsets, and region zone IDs;
  locale-backed era `G`, format/standalone month `M`/`L`, weekday `E`, AM/PM
  `a`, and format/standalone quarter `Q`/`q` text in full, short, and narrow
  styles, with locale-sensitive parsing and immutable `withLocale` behavior;
  chronology-aware localized era and month text across ISO, Japanese, Hijrah,
  Minguo, and Thai Buddhist calendars;
  locale-defined week-based year `Y`, week `w`/`W`, and numeric or textual
  day-of-week `e`/`c` patterns, including reduced years, adjacent parsing, and
  strict/smart/lenient date resolution;
  locale-backed flexible day-period `B` text in full, short, and narrow
  styles, with midpoint resolution, 12-hour disambiguation, and resolver-style
  conflict handling;
  lazy `ofLocalizedDate`, `ofLocalizedTime`, `ofLocalizedDateTime`, and
  CLDR-template `ofLocalizedPattern` factories backed by platform locale
  patterns and chronology-aware style/template lookup; localized specific and
  generic zone-name patterns `z`/`v`, including
  daylight-aware formatting, localized parsing, and preferred-zone
  disambiguation;
  Java-compatible aggregate and partial time-field
  normalization for nano/micro/milli (including zero fractional fields for
  second-only parses), second/minute-of-day, and AM/PM clocks;
  `DateTimeFormatterBuilder` pattern/literal and generic numeric-value
  composition, sign/width controls, integer- and chronology-base-date reduced
  value windows, fixed-range fractions, immutable custom field-text maps and
  locale-backed `appendText` overloads,
  default and explicit-width instant formatting/parsing, all custom offset-ID
  patterns, FULL/SHORT `GMT` localized offsets and `O` patterns, explicit
  localized date/time style and template composition and pattern lookup,
  specific and generic localized zone-name text with preferred-zone parsing,
  region/zone/offset query modes, chronology-ID and localized chronology-name
  formatting/parsing with chronology-aware date and reduced-value resolution,
  adjacent fixed-width
  parsing, one-shot custom-character padding and `p` pattern modifiers, nested
  optional sections through builder methods and bracket patterns, required and
  optional reuse of complete formatter graphs through `append` and `appendOptional`
  with outer resolver/chronology/zone/decimal overrides and shared sequential
  parser settings,
  sequential case/strictness parser controls, parse defaults, unresolved field
  retention, and immutable formatter snapshots; plus
  Java-compatible `ISO_LOCAL_DATE`, `ISO_LOCAL_TIME`,
  `ISO_LOCAL_DATE_TIME`, `ISO_INSTANT`, `ISO_OFFSET_DATE`, `ISO_DATE`,
  `ISO_TIME`, `ISO_DATE_TIME`, `ISO_OFFSET_TIME`, `ISO_OFFSET_DATE_TIME`, and
  `ISO_ZONED_DATE_TIME` constants, plus `ISO_ORDINAL_DATE`, `ISO_WEEK_DATE`,
  `BASIC_ISO_DATE`, and the English `RFC_1123_DATE_TIME`; parsed
  date/time/offset/region-zone query retention; leap-second and excess-day
  parsed-state queries; and formatter overloads on their corresponding value
  types
- `DayOfWeek` ISO numbering, temporal conversion and adjustment, day precision,
  wraparound arithmetic, and localized display names
- `Month` ISO numbering, temporal conversion including epoch-day conversion
  from non-ISO chronologies, ISO-guarded adjustment, chronology/month precision
  queries, calendar metadata, wraparound arithmetic, and localized display names
- `Era` and `IsoEra`, including era precision and localized display names
- `MinguoEra`, `ThaiBuddhistEra`, `JapaneseEra`, and `HijrahEra`, including
  Java-compatible numeric values, Japanese title-case lookup and defensive era
  enumeration, the single-era Hijrah range, and chronology-specific localized
  display names
- `Chronology` identity, lookup, ordering, era/leap/range contracts, plus the
  recommended `AbstractChronology` base with shared resolution and value
  behavior, and the `IsoChronology`, `JapaneseChronology`, `HijrahChronology`,
  `MinguoChronology`, and `ThaiBuddhistChronology` singletons with generic
  date/date-time factories, locale calendar lookup, JVM/Android
  `ServiceLoader` discovery, localized display names, strict/smart/lenient
  field-map date resolution, clock injection, period creation, epoch-second
  conversion, and Java-compatible default period and instant-zone factories
  for custom chronologies
- `JapaneseDate`, `HijrahDate`, `MinguoDate`, and `ThaiBuddhistDate` factories,
  fields and refined ranges, calendar arithmetic, periods, generic local/zoned
  date-time composition, and Java-compatible value semantics, including
  Japanese era transitions and short transition years and the bundled OpenJDK
  Umm al-Qura month table for 1300 through 1600 AH
- `ChronoLocalDate` chronology, era, leap-year and length contracts, generic
  conversion, chronology-validating covariant adjustment and arithmetic
  defaults, standard queries, epoch-day adjustment, formatter-based output,
  Java-compatible default local-time composition, chronology-aware and
  timeline ordering, with `LocalDate` integration
- `ChronoLocalDateTime` chronology/date/time contracts, generic conversion and
  chronology factories, Java-compatible int/long temporal fields,
  chronology-validating covariant adjustment and amount arithmetic defaults,
  cross-chronology local-timeline adjustment, standard queries, epoch/instant
  conversion, adjustment, formatter-based output, chronology-aware and
  local-timeline ordering, with `LocalDateTime` integration
- `ChronoZonedDateTime` chronology/local/offset/zone contracts, generic
  conversion and chronology factories, chronology-validating covariant
  adjustment and amount arithmetic defaults, cross-chronology local-timeline
  adjustment, zone changes, standard queries, instant conversion,
  formatter-based output, structural and instant-timeline ordering, with
  `ZonedDateTime` integration
- `ChronoPeriod` chronology, unit, sign, arithmetic, normalization, application,
  and generic date-difference contracts, with `Period` integration
- `Year` core value, leap-year, Java-compatible int/long temporal fields and
  field-adjustment validation, year-scale arithmetic, and
  comparison behavior, including non-ISO epoch-day conversion, ISO-guarded
  temporal adjustment, default parsing, and `atDay` date production plus
  system-default/injected-clock/explicit-zone current-value factories and
  `MonthDay` validation/date production and formatter-based parsing/formatting
- `MonthDay` value, factories, Java-compatible int/long temporal fields,
  replacement, year validation,
  non-ISO epoch-day conversion, date production and ISO-guarded adjustment,
  ordering, strict ISO parsing, and ISO text output, including system-default/
  injected-clock/explicit-zone current-value factories and formatter-based
  parsing/formatting
- `YearMonth` value, factories, Java-compatible int/long temporal fields,
  replacement, checked month/year-scale arithmetic, complete-unit differences,
  non-ISO epoch-day conversion, date production and ISO-guarded adjustment,
  ordering, strict ISO parsing, ISO text output, and system-default/
  injected-clock/explicit-zone current-value factories and formatter-based
  parsing/formatting
- `LocalDate` core value, full-range epoch and instant/zone conversion,
  Java-compatible int/long temporal fields, replacement, checked calendar
  arithmetic, complete-unit differences, and timeline and calendar-period
  comparison, with lazy exclusive date sequences
  using day or calendar-period steps; local time composition, local-time/offset
  epoch-second conversion, earliest-valid zoned start-of-day composition,
  strict ISO parsing, and ISO text output, with system-default/injected-clock/
  explicit-zone current-value factories and formatter overloads
- `Period` factories, parsing, value behavior, chronology-validated temporal
  integration, checked arithmetic, normalization, and concrete/generic `between`
- `LocalTime` core value, validated component/day and instant/zone factories,
  Java-compatible int/long temporal fields, replacement, truncation, wraparound
  arithmetic, complete-unit differences, ordering, strict ISO parsing, ISO text
  output, offset/date composition, local-date/offset epoch-second conversion,
  and system-default/
  injected-clock/explicit-zone current-value factories and formatter overloads
- `LocalDateTime` immutable composition, factories, Java-compatible int/long
  local field access, epoch/fixed-offset and instant/zone conversion,
  replacement, truncation, checked date/time arithmetic, complete-unit
  differences, boundaries, ordering, strict ISO parsing, ISO text output, and
  system-default, injected-clock, and explicit-zone current-value factories and
  formatter overloads
- `Duration` with the complete Java 25 public API surface, including parsing,
  temporal integration, checked arithmetic, conversions, truncation, and
  `between`
- `Instant` value and boundary constants, normalized epoch factories,
  Java-compatible int/long temporal fields, replacement, truncation, checked
  precise-unit arithmetic, complete-unit and duration differences,
  epoch-millisecond conversion, adjustment, ordering, hashing, strict ISO
  parsing, and ISO text output, including fixed-offset/region-zone composition
  and system/injected-clock current values
- `ZoneOffset` factories, validation, normalized ID parsing and formatting,
  quarter-hour canonicalization, `ZoneId` integration, Java-compatible int/long
  offset-field and query integration, adjustment, and ordering
- `ZoneId` fixed-offset and `UTC`/`GMT`/`UT` prefixed identifiers, alias-map
  resolution, provider-backed region IDs, normalization, available-ID access,
  bundled IANA TZDB 2026b resolution, system-default lookup, localized display
  names, and zone/zone-offset temporal queries
- `ZoneRules` fixed and variable in-memory rule sets, historic standard/wall
  transitions, recurring future rules, instant and ambiguous local-time
  resolution, daylight-saving calculations, transition navigation, and full
  rule-list value semantics
- `ZoneRulesProvider` registration, conflict detection, region/rule/version
  lookup, dynamic no-cache rules, refresh support, and automatic registration
  of the bundled IANA TZDB 2026b database, plus `ServiceLoader` discovery on
  JVM and Android
- `ZoneOffsetTransition` gap/overlap values, validated factories, instant and
  local timeline conversion, duration, offset validation, ordering, hashing,
  and Java-compatible text output
- `ZoneOffsetTransitionRule` recurring month/day/weekday rules, UTC/standard/
  wall-time definitions, end-of-day handling, annual transition creation,
  validation, value semantics, and Java-compatible text output
- `OffsetTime` immutable composition, component and instant/zone factories,
  local and offset fields, offset conversion with same-local and same-instant
  semantics, replacement, truncation, wraparound arithmetic, complete-unit
  differences, timeline and structural ordering, epoch-second conversion,
  strict ISO parsing, and ISO text output, including system-default,
  injected-clock, and explicit-zone current values and formatter overloads
- `OffsetDateTime` immutable composition, factories, fixed-offset and region-zone
  instant conversion, same-instant/similar-local region-zone composition,
  Java-compatible standard int/long fields and units, replacement, truncation,
  checked local arithmetic, complete-unit differences across offsets, timeline
  and structural ordering, strict ISO parsing, ISO text output, and
  system-default/injected-clock/explicit-zone current values and formatter
  overloads
- `ZonedDateTime` local, strict, and instant factories; gap shifting and
  overlap preference/switching; date-based versus elapsed-time arithmetic;
  Java-compatible int/long temporal fields, replacement, truncation,
  complete-unit differences, zone conversion, instant/offset conversion,
  timeline and structural ordering, queries, strict ISO parsing,
  Java-compatible text output, and system-default/injected-clock/explicit-zone
  current values and formatter overloads
- `Clock` system clocks for the default and explicit zones, fixed and offset
  clocks, validated nanosecond-to-minute tick clocks, millisecond access, zone
  replacement, and Java-compatible value/text semantics
- `InstantSource` system, fixed, offset, and tick sources, default millisecond
  conversion, arbitrary-source zone-to-clock bridging, and Java-compatible
  reuse of clock implementations
- temporal interfaces and exceptions, including `TemporalField` localized-name
  defaults and parsed-field resolution, plus all standard chronology, zone,
  offset, local-date, local-time, and precision queries with Java-compatible
  singleton names, strict zone-query delegation, and type-aware temporal-unit
  support probes
- `TemporalAdjusters` complete date-adjuster utility surface
- `JulianFields` Julian day, modified Julian day, and Rata Die fields with
  full-range epoch conversion, adjustment, and chronology-aware parsed-field
  resolution
- `IsoFields` quarter-of-year and ISO week-based-year fields and units, with
  leap-year ranges, field adjustment, calendar arithmetic, and strict/smart/
  lenient quarter and week-date resolution plus localized week display names
- `WeekFields` canonical explicit week definitions for every first-day and
  minimal-days combination, locale-backed rule selection through JVM,
  Android, and iOS locale data, all five localized computed fields, and
  strict/smart/lenient parsed-field resolution
- `ValueRange`
- `ChronoUnit`
- `ChronoField` metadata, validation, temporal dispatch, canonical names, and
  locale-backed display names with Java-compatible fallbacks

Ongoing work is compatibility hardening: expanding differential inputs,
tracking future Java and timezone-data releases, and improving platform locale
coverage without changing Java 25 semantics.

The bundled TZDB source is generated reproducibly from the OpenJDK 21
`lib/tzdb.dat` file by `tools/generate-tzdb-data.main.kts`.

The bundled Umm al-Qura month table is generated reproducibly from OpenJDK 21's
`hijrah-config-Hijrah-umalqura_islamic-umalqura.properties` module resource by
`tools/generate-hijrah-data.main.kts`.

## License

Apache License 2.0. See `LICENSE`.
