# grogu-time

`grogu-time` is a Kotlin Multiplatform port of the Java 21 `java.time` API under
the `io.heapy.grogu.time` package.

The project is under active development and is not yet a complete port.

## Build

The repository uses JetBrains Kotlin Toolchain 0.11.0 and its checked-in
wrapper. No Gradle installation is required.

```shell
./kotlin test
```

The current module targets JVM, Android, iOS ARM64, iOS Simulator ARM64, and
iOS x64. Production code and behavioral tests live in common Kotlin. JVM-only
differential tests compare observable behavior with Java 21 `java.time`.

## Compatibility contract

- Package root: `io.heapy.grogu.time`
- Behavioral reference: Java 21 `java.time`
- API style: Kotlin properties and operators where they preserve Java
  semantics, with Java-named operations retained when useful
- Arithmetic: checked overflow and normalized values must match Java
- Development: red-green-refactor slices, with each green slice committed

## Coverage

Implemented foundations:

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
  `ofPattern` date/time fields, including ordinal day `D`, aligned week `F`,
  clock-hour `k`/`K`/`h`, milli/nano-of-day `A`/`N`, raw nano `n`, exact
  fractions, modified-Julian day `g`, numeric quarter `Q`/`q`, reduced years,
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
  normalization for nano/micro/milli, second/minute-of-day, and AM/PM clocks;
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
- `DayOfWeek`
- `Month`
- `Era` and `IsoEra`
- `MinguoEra`, `ThaiBuddhistEra`, `JapaneseEra`, and `HijrahEra`, including
  Java-compatible numeric values, Japanese title-case lookup and defensive era
  enumeration, and the single-era Hijrah range
- `Chronology` identity, lookup, ordering, era/leap/range contracts, plus the
  `IsoChronology`, `JapaneseChronology`, `HijrahChronology`,
  `MinguoChronology`, and `ThaiBuddhistChronology` singletons with generic
  date/date-time factories, locale calendar lookup, localized display names,
  strict/smart/lenient field-map date resolution, clock injection, period
  creation, and epoch-second conversion
- `JapaneseDate`, `HijrahDate`, `MinguoDate`, and `ThaiBuddhistDate` factories,
  fields and refined ranges, calendar arithmetic, periods, generic local/zoned
  date-time composition, and Java-compatible value semantics, including
  Japanese era transitions and short transition years and the bundled OpenJDK
  Umm al-Qura month table for 1300 through 1600 AH
- `ChronoLocalDate` chronology, era, leap-year and length contracts, generic
  conversion, covariant date arithmetic, standard queries, epoch-day adjustment,
  formatter-based output, chronology-aware and timeline ordering, with
  `LocalDate` integration
- `ChronoLocalDateTime` chronology/date/time contracts, generic conversion and
  chronology factories, covariant arithmetic, standard queries, epoch/instant
  conversion, adjustment, formatter-based output, chronology-aware and
  local-timeline ordering, with `LocalDateTime` integration
- `ChronoZonedDateTime` chronology/local/offset/zone contracts, generic
  conversion and chronology factories, covariant arithmetic and zone changes,
  standard queries, instant conversion, formatter-based output, structural and
  instant-timeline ordering, with `ZonedDateTime` integration
- `ChronoPeriod` chronology, unit, sign, arithmetic, normalization, application,
  and generic date-difference contracts, with `Period` integration
- `Year` core value, leap-year, temporal-field, year-scale arithmetic, and
  comparison behavior, including default parsing and `atDay` date production
  plus system-default/injected-clock/explicit-zone current-value factories and
  `MonthDay` validation/date production and formatter-based parsing/formatting
- `MonthDay` value, factories, temporal fields, replacement, year validation,
  date production and adjustment, ordering, strict ISO parsing, and ISO text
  output, including system-default/injected-clock/explicit-zone current-value
  factories and formatter-based parsing/formatting
- `YearMonth` value, factories, temporal fields, replacement, checked
  month/year-scale arithmetic, complete-unit differences, date production and
  adjustment, ordering, strict ISO parsing, ISO text output, and
  system-default/injected-clock/explicit-zone current-value factories and
  formatter-based parsing/formatting
- `LocalDate` core value, full-range epoch and instant/zone conversion, temporal fields,
  replacement, checked calendar arithmetic, complete-unit differences, and
  timeline and calendar-period comparison, with lazy exclusive date sequences
  using day or calendar-period steps; local time composition, local-time/offset
  epoch-second conversion, earliest-valid zoned start-of-day composition,
  strict ISO parsing, and ISO text output, with system-default/injected-clock/
  explicit-zone current-value factories and formatter overloads
- `Period` factories, parsing, value behavior, chronology-validated temporal
  integration, checked arithmetic, normalization, and concrete/generic `between`
- `LocalTime` core value, validated component/day and instant/zone factories,
  temporal fields, replacement, truncation, wraparound arithmetic, complete-unit
  differences, ordering, strict ISO parsing, ISO text output, offset/date
  composition, and system-default/injected-clock/explicit-zone current-value
  factories and formatter overloads
- `LocalDateTime` immutable composition, factories, local field access,
  epoch/fixed-offset and instant/zone conversion, replacement, truncation,
  checked date/time arithmetic, complete-unit differences, boundaries,
  ordering, strict ISO parsing, ISO text output, and system-default,
  injected-clock, and explicit-zone current-value factories and formatter
  overloads
- `Duration` with the complete Java 21 public API surface, including parsing,
  temporal integration, checked arithmetic, conversions, truncation, and
  `between`
- `Instant` value and boundary constants, normalized epoch factories,
  temporal fields, replacement, truncation, checked precise-unit arithmetic,
  complete-unit and duration differences, epoch-millisecond conversion,
  adjustment, ordering, hashing, strict ISO parsing, and ISO text output,
  including fixed-offset/region-zone composition and system/injected-clock
  current values
- `ZoneOffset` factories, validation, normalized ID parsing and formatting,
  quarter-hour canonicalization, `ZoneId` integration, offset-field/query
  integration, adjustment, and Java-compatible ordering
- `ZoneId` fixed-offset and `UTC`/`GMT`/`UT` prefixed identifiers, alias-map
  resolution, provider-backed region IDs, normalization, available-ID access,
  bundled IANA TZDB 2025a resolution, system-default lookup, localized display
  names, and zone/zone-offset temporal queries
- `ZoneRules` fixed and variable in-memory rule sets, historic standard/wall
  transitions, recurring future rules, instant and ambiguous local-time
  resolution, daylight-saving calculations, transition navigation, and full
  rule-list value semantics
- `ZoneRulesProvider` registration, conflict detection, region/rule/version
  lookup, dynamic no-cache rules, refresh support, and automatic registration
  of the bundled IANA TZDB 2025a database, plus `ServiceLoader` discovery on
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
- `OffsetDateTime` immutable composition, factories, fixed-offset/instant
  conversion, same-instant/similar-local region-zone composition, all standard
  fields and units, replacement, truncation, checked local arithmetic,
  complete-unit differences across offsets, timeline and structural ordering,
  strict ISO parsing, ISO text output, and system-default/injected-clock/
  explicit-zone current values and formatter overloads
- `ZonedDateTime` local, strict, and instant factories; gap shifting and
  overlap preference/switching; date-based versus elapsed-time arithmetic;
  temporal fields, replacement, truncation, complete-unit differences, zone
  conversion, instant/offset conversion, timeline and structural ordering,
  queries, strict ISO parsing, Java-compatible text output, and system-default/
  injected-clock/explicit-zone current values and formatter overloads
- `Clock` system clocks for the default and explicit zones, fixed and offset
  clocks, validated nanosecond-to-minute tick clocks, millisecond access, zone
  replacement, and Java-compatible value/text semantics
- `InstantSource` system, fixed, offset, and tick sources, default millisecond
  conversion, arbitrary-source zone-to-clock bridging, and Java-compatible
  reuse of clock implementations
- temporal interfaces and exceptions, including `TemporalField` parsed-field
  resolution, plus all standard chronology, zone, offset, local-date,
  local-time, and precision queries
- `TemporalAdjusters` complete date-adjuster utility surface
- `JulianFields` Julian day, modified Julian day, and Rata Die fields with
  full-range epoch conversion, adjustment, and chronology-aware parsed-field
  resolution
- `IsoFields` quarter-of-year and ISO week-based-year fields and units, with
  leap-year ranges, field adjustment, calendar arithmetic, and strict/smart/
  lenient quarter and week-date resolution
- `WeekFields` canonical explicit week definitions for every first-day and
  minimal-days combination, locale-backed rule selection through JVM,
  Android, and iOS locale data, all five localized computed fields, and
  strict/smart/lenient parsed-field resolution
- `ValueRange`
- `ChronoUnit`
- `ChronoField`

Remaining work includes formatters and parsers beyond the implemented ISO
defaults and amount parsers, and complete cross-type conformance coverage.

The bundled TZDB source is generated reproducibly from the OpenJDK 21
`lib/tzdb.dat` file by `tools/generate-tzdb-data.rb`.

The bundled Umm al-Qura month table is generated reproducibly from OpenJDK 21's
`hijrah-config-Hijrah-umalqura_islamic-umalqura.properties` module resource by
`tools/generate-hijrah-data.rb`.
