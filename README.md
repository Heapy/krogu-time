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
  comparison behavior (clock, formatting, and date-producing APIs remain)
- `LocalDate` core value, full-range epoch conversion, temporal fields,
  replacement, checked calendar arithmetic, complete-unit differences, and
  timeline and calendar-period comparison, including local time composition,
  strict ISO parsing, and ISO text output (clock, formatter overloads, queries,
  and zone APIs remain)
- `Period` factories, parsing, value behavior, temporal integration, checked
  arithmetic, normalization, and `between` (chronology integration remains)
- `LocalTime` core value, validated factories, day conversions, temporal
  fields, replacement, truncation, wraparound arithmetic, complete-unit
  differences, ordering, strict ISO parsing, and ISO text output (formatter
  overloads, queries, and offset/zone APIs remain)
- `LocalDateTime` immutable composition, factories, local field access,
  conversion, replacement, truncation, checked date/time arithmetic,
  complete-unit differences, boundaries, ordering, and ISO text output
  (parsing/formatters, queries, and zone APIs remain)
- `Duration` with the complete Java 21 public API surface, including parsing,
  temporal integration, checked arithmetic, conversions, truncation, and
  `between`
- temporal interfaces and exceptions
- `ValueRange`
- `ChronoUnit`
- `ChronoField`

Remaining work includes standard temporal queries and adjusters, other local
and offset date/time types, instants and clocks, chronology integration, zones
and rules, formatters and parsers beyond the implemented ISO defaults and
amount parsers, alternate chronologies, and complete cross-type conformance
coverage.
