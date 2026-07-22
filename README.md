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
- `Chronology` identity, lookup, ordering, era/leap/range contracts, and the
  `IsoChronology` singleton with generic and ISO date/date-time factories, clock
  injection, period creation, and epoch-second conversion
- `ChronoLocalDate` chronology, era, leap-year and length contracts, generic
  conversion, covariant date arithmetic, standard queries, epoch-day adjustment,
  chronology-aware and timeline ordering, with `LocalDate` integration
- `ChronoLocalDateTime` chronology/date/time contracts, generic conversion and
  chronology factories, covariant arithmetic, standard queries, epoch/instant
  conversion, adjustment, chronology-aware and local-timeline ordering, with
  `LocalDateTime` integration
- `ChronoPeriod` chronology, unit, sign, arithmetic, normalization, application,
  and generic date-difference contracts, with `Period` integration
- `Year` core value, leap-year, temporal-field, year-scale arithmetic, and
  comparison behavior, including default parsing and `atDay` date production
  plus injected-clock/explicit-zone current-value factories and `MonthDay`
  validation/date production (system-default current values, formatter
  overloads remain)
- `MonthDay` value, factories, temporal fields, replacement, year validation,
  date production and adjustment, ordering, strict ISO parsing, and ISO text
  output, including injected-clock/explicit-zone current-value factories
  (system-default current values and formatter overloads remain)
- `YearMonth` value, factories, temporal fields, replacement, checked
  month/year-scale arithmetic, complete-unit differences, date production and
  adjustment, ordering, strict ISO parsing, ISO text output, and
  injected-clock/explicit-zone current-value factories (system-default current
  values and formatter overloads remain)
- `LocalDate` core value, full-range epoch conversion, temporal fields,
  replacement, checked calendar arithmetic, complete-unit differences, and
  timeline and calendar-period comparison, including local time composition,
  earliest-valid zoned start-of-day composition, strict ISO parsing, and ISO
  text output, with injected-clock/explicit-zone current-value factories
  (system-default current values and formatter overloads remain)
- `Period` factories, parsing, value behavior, chronology-validated temporal
  integration, checked arithmetic, normalization, and concrete/generic `between`
- `LocalTime` core value, validated factories, day conversions, temporal
  fields, replacement, truncation, wraparound arithmetic, complete-unit
  differences, ordering, strict ISO parsing, ISO text output, offset/date
  composition, and injected-clock/explicit-zone current-value factories
  (system-default current values and formatter overloads remain)
- `LocalDateTime` immutable composition, factories, local field access,
  epoch/fixed-offset/zone conversion, replacement, truncation, checked date/time
  arithmetic, complete-unit differences, boundaries, ordering, strict ISO
  parsing, ISO text output, and injected-clock/explicit-zone current-value
  factories (system-default current values and formatter overloads remain)
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
  text output, including injected-clock/explicit-zone current values
  (system-default current values and formatter overloads remain)
- `OffsetDateTime` immutable composition, factories, fixed-offset/instant
  conversion, same-instant/similar-local region-zone composition, all standard
  fields and units, replacement, truncation, checked local arithmetic,
  complete-unit differences across offsets, timeline and structural ordering,
  strict ISO parsing, ISO text output, and injected-clock/explicit-zone current
  values (system-default current values and formatter overloads remain)
- `ZonedDateTime` local, strict, and instant factories; gap shifting and
  overlap preference/switching; date-based versus elapsed-time arithmetic;
  temporal fields, replacement, truncation, complete-unit differences, zone
  conversion, instant/offset conversion, timeline and structural ordering,
  queries, strict ISO parsing, Java-compatible text output, and injected-clock/
  explicit-zone current values (system-default current values and formatter
  overloads remain)
- `Clock` system clocks for explicit zones, fixed and offset clocks, validated
  nanosecond-to-minute tick clocks, millisecond access, zone replacement, and
  Java-compatible value/text semantics (system-default-zone lookup remains)
- `InstantSource` system, fixed, offset, and tick sources, default millisecond
  conversion, arbitrary-source zone-to-clock bridging, and Java-compatible
  reuse of clock implementations
- temporal interfaces and exceptions, plus all standard chronology, zone,
  offset, local-date, local-time, and precision queries
- `TemporalAdjusters` complete date-adjuster utility surface
- `ValueRange`
- `ChronoUnit`
- `ChronoField`

Remaining work includes the generic chronology zoned-date-time type hierarchy,
no-argument system-default current-value
factories and system-default-zone lookup, a bundled region time-zone database,
formatters and parsers beyond the implemented ISO defaults and amount parsers,
alternate chronologies, and complete cross-type conformance coverage.
