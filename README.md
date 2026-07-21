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
- `Duration` with the complete Java 21 public API surface, including parsing,
  temporal integration, checked arithmetic, conversions, truncation, and
  `between`
- temporal interfaces and exceptions
- `ValueRange`
- `ChronoUnit`
- `ChronoField`

Remaining work includes temporal queries and adjusters, local and offset
date/time types, instants and clocks, periods, zones and rules, formatters and
parsers beyond duration parsing, alternate chronologies, and complete
cross-type conformance coverage.
