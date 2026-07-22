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
- `DayOfWeek`
- `Month`
- `Era` and `IsoEra`
- `Year` core value, leap-year, temporal-field, year-scale arithmetic, and
  comparison behavior, including default parsing and `atDay` date production
  plus `MonthDay` validation and date production (clock, formatter overloads,
  and temporal-query integration remain)
- `MonthDay` value, factories, temporal fields, replacement, year validation,
  date production and adjustment, ordering, strict ISO parsing, and ISO text
  output (clock and formatter overloads remain)
- `YearMonth` value, factories, temporal fields, replacement, checked
  month/year-scale arithmetic, complete-unit differences, date production and
  adjustment, ordering, strict ISO parsing, and ISO text output (clock,
  formatter overloads, and temporal-query integration remain)
- `LocalDate` core value, full-range epoch conversion, temporal fields,
  replacement, checked calendar arithmetic, complete-unit differences, and
  timeline and calendar-period comparison, including local time composition,
  strict ISO parsing, and ISO text output (clock, formatter overloads, queries,
  and zone-region APIs remain)
- `Period` factories, parsing, value behavior, temporal integration, checked
  arithmetic, normalization, and `between` (chronology integration remains)
- `LocalTime` core value, validated factories, day conversions, temporal
  fields, replacement, truncation, wraparound arithmetic, complete-unit
  differences, ordering, strict ISO parsing, and ISO text output (formatter
  overloads, queries, and offset/zone APIs remain)
- `LocalDateTime` immutable composition, factories, local field access,
  epoch/fixed-offset conversion, replacement, truncation, checked date/time
  arithmetic, complete-unit differences, boundaries, ordering, strict ISO
  parsing, and ISO text output (formatter overloads, queries, and zone-region
  APIs remain)
- `Duration` with the complete Java 21 public API surface, including parsing,
  temporal integration, checked arithmetic, conversions, truncation, and
  `between`
- `Instant` value and boundary constants, normalized epoch factories,
  temporal fields, replacement, truncation, checked precise-unit arithmetic,
  complete-unit and duration differences, epoch-millisecond conversion,
  adjustment, ordering, hashing, strict ISO parsing, and ISO text output (clock
  factories, queries, and zone-region composition remain)
- `ZoneOffset` factories, validation, normalized ID parsing and formatting,
  quarter-hour canonicalization, `ZoneId` integration, offset-field/query
  integration, adjustment, and Java-compatible ordering
- `ZoneId` fixed-offset and `UTC`/`GMT`/`UT` prefixed identifiers, alias-map
  resolution, provider-backed region IDs, normalization, available-ID access,
  and zone/zone-offset temporal queries (system defaults and display names
  remain)
- `ZoneRules` fixed and variable in-memory rule sets, historic standard/wall
  transitions, recurring future rules, instant and ambiguous local-time
  resolution, daylight-saving calculations, transition navigation, and full
  rule-list value semantics (external TZDB providers remain)
- `ZoneRulesProvider` registration, conflict detection, region/rule/version
  lookup, dynamic no-cache rules, and refresh support (automatic service
  loading and a bundled TZDB provider remain)
- `ZoneOffsetTransition` gap/overlap values, validated factories, instant and
  local timeline conversion, duration, offset validation, ordering, hashing,
  and Java-compatible text output
- `ZoneOffsetTransitionRule` recurring month/day/weekday rules, UTC/standard/
  wall-time definitions, end-of-day handling, annual transition creation,
  validation, value semantics, and Java-compatible text output
- `OffsetTime` immutable composition, factories, local and offset fields,
  offset conversion with same-local and same-instant semantics, replacement,
  truncation, wraparound arithmetic, complete-unit differences, timeline and
  structural ordering, epoch-second conversion, strict ISO parsing, and ISO
  text output (clock/zone factories and formatter overloads remain)
- `OffsetDateTime` immutable composition, factories, fixed-offset/instant
  conversion, all standard fields and units, replacement, truncation, checked
  local arithmetic, complete-unit differences across offsets, timeline and
  structural ordering, strict ISO parsing, and ISO text output (clock/zone
  factories and formatter overloads remain)
- temporal interfaces and exceptions, plus the standard offset query
- `TemporalAdjusters` complete date-adjuster utility surface
- `ValueRange`
- `ChronoUnit`
- `ChronoField`

Remaining work includes the other standard temporal queries, zoned date/time
types, clocks, chronology integration, region-backed zone rules, formatters and
parsers beyond the implemented ISO defaults and amount parsers, alternate
chronologies, and complete cross-type conformance coverage.
