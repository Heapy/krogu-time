# Architecture

## One module, five targets

The repository is a single JetBrains Kotlin Toolchain module rooted at
`module.yaml`. It builds one `kmp/lib` product for JVM, Android, iOS ARM64, iOS
Simulator ARM64, and iOS x64.

There is no `project.yaml`: a single-module project does not need one.

## The port lives in common Kotlin

Almost the whole library is common code under `src/`, in the package
`io.heapy.krogu.time`:

| Path | Contents |
| --- | --- |
| `src/…/time` | The core value types: `Instant`, `Duration`, `LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetTime`, `OffsetDateTime`, `ZonedDateTime`, `Period`, `Year`, `YearMonth`, `MonthDay`, `Month`, `DayOfWeek`, `ZoneId`, `ZoneOffset`, `Clock` |
| `src/…/time/temporal` | The `Temporal` abstraction: fields, units, queries, adjusters, `WeekFields`, `ValueRange` |
| `src/…/time/chrono` | Calendar systems: ISO, Hijrah, Japanese, Minguo, Thai Buddhist, plus the `Chronology` registry |
| `src/…/time/format` | `DateTimeFormatter`, its builder, pattern parsing, `DecimalStyle`, and the style enums |
| `src/…/time/zone` | `ZoneRules`, transitions, transition rules, and the bundled TZDB provider |
| `src/…/time/internal` | Shared helpers that are not part of the public surface |

The arithmetic, the calendar rules, the formatter, and the zone-rules engine are
all common. No target has its own copy of that logic.

## Platform code is only what the platform must supply

A handful of `expect` declarations sit in `src/`, and each target supplies the
`actual`. They cover the four things a date-time library cannot compute on its
own:

| Concern | `expect` declaration | JVM | Android | iOS |
| --- | --- | --- | --- | --- |
| System time zone | `systemDefaultZoneId` | `java.time.ZoneId` | `java.util.TimeZone` | `NSTimeZone` |
| Default locale and its week rules | `PlatformLocale` | `java.util.Locale` | `java.util.Locale` | `NSLocale`, `NSCalendar` |
| Localized text and patterns | `PlatformText`, `PlatformLocalizedPattern`, `PlatformChronologyText`, `PlatformZoneText`, `PlatformDayPeriod` | JDK CLDR data | JDK CLDR data | `NSDateFormatter` CLDR data |
| Third-party `Chronology` and `ZoneRulesProvider` discovery | `loadChronologies`, `loadZoneRulesProviders` | `ServiceLoader` | `ServiceLoader` | none, the list is empty |

Source sets:

- `src@jvm`, `src@android`, `src@ios` hold the per-target `actual`s.
- `src@jvmAndAndroid` is an alias for the two JDK-backed targets. It holds what
  they share, such as the `java.text.Format` adapter and `ParsePosition`.

`ZoneRules` are the exception: the rules themselves are not read from the
platform. `src/…/time/zone/TzdbData.kt` carries a bundled copy of the IANA
database, generated from a JDK `lib/tzdb.dat` by
`tools/generate-tzdb-data.main.kts`. Every target resolves zone rules from that
same data, so a zone offset is identical everywhere.

`tools/generate-hijrah-data.main.kts` generates the Hijrah calendar variant data
the same way.

## Two JDK versions, on purpose

`module.yaml` pins two separate knobs:

- `settings.jvm.jdk.version: 25` is the JDK that compiles the code and runs the
  differential tests. It sets the behavioral reference of the port.
- `settings.jvm.release: 21` is the bytecode level of the published artifact, so
  the library still loads on a Java 21 JVM.

`test-settings.jvm.release: 25` lets the differential tests see the full JDK 25
API they compare against.

## Tests

- `test/` holds common behavioral tests. They run on every target.
- `test@jvm/` holds the differential tests. Each one calls `java.time` and the
  port with the same input and asserts the same result.
- `test@android/` holds the one Android-only test, which checks service
  loading on that platform. The rest of the Android run is the common suite.
