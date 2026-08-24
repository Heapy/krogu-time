# TCK spike findings

Spike goal: decide whether the OpenJDK `java.time` TCK can run against
`krogu-time` in CI, and at what cost.

Reference JDK 25.0.4, the JDK that runs the JVM differential tests and so
sets the behavioral reference of this port. TCK sources from `openjdk/jdk25u`
at tag `jdk-25.0.4.1-ga`. Both are pinned in `tools/run-tck.main.kts` and must
move together with `jvm.jdk.version` in `module.yaml`.

Reproduce with `./kotlin build && kotlinr tools/run-tck.main.kts`. The numbers below
come from that script, run twice with the same result.

## Result

It works. Converted TCK, run against the port:

```
Total tests run: 20915, Passes: 20867, Failures: 48
```

113 of 155 TCK/test files ran; 42 were dropped because the converter cannot
handle them yet, and the script lists every one. The conversion is fully mechanical, driven by
the built jar rather than a hand-written rule list.

## Gates checked

| Gate | Result |
| --- | --- |
| License of TCK sources | **GPLv2 only, no Classpath exception.** Do not vendor into this Apache-2.0 repo. The script fetches at run time and discards. |
| Interface default methods | Real JVM default methods, not `DefaultImpls`. Java can implement `TemporalAccessor`/`Temporal` anonymously. No blocker. |
| `@JvmStatic` needed? | **No.** Rewriting call sites to `LocalDate.Companion.of(...)` and `LocalDate.Companion.getMIN()` compiles clean. The library needs no change. |

## What the converter does

Rules are derived from the jar, not written by hand:

- every `$Companion` class is read with `javap`; its members become
  `Type.Companion.member(...)` and `Type.Companion.getCONST()`
- every Kotlin `object` singleton becomes `Type.INSTANCE.member(...)`
- enum constants are left alone (they are real statics)
- a simple name imported from outside the port is left alone
- `java.time` becomes `io.heapy.krogu.time`, never inside string literals
- the noun accessors from `MIGRATION_NOTES.md` become `getXxx()`

On the full tree that is 9605 rewrites across 155 files.

## Failure triage

### Harness artifacts, not port bugs

- `test_immutable` (~14). The TCK asserts every declared constructor is
  private. The real constructor is private; Kotlin adds a synthetic public
  `(int, int, int, DefaultConstructorMarker)` constructor whose name has no
  `$`, so the TCK's filter does not skip it.
- `test_serialization` (5). The port implements no `java.io.Serializable`.
  Permanent exclusion.
- Coptic `ServiceLoader` (2). The TCK's service fixture is not wired up yet.
- `TestClock_System` (2). `NoClassDefFoundError` from jtreg infrastructure.
- `TestMutableZoneRules.testLength` (1). Java copies the rule list through
  `toArray()` and rejects the result when it holds more than 16 rules. The
  test passes a `List` whose `toArray()` returns 17 elements while `size()`
  reports 1, which is a JVM-only hardening: Kotlin's common `List` has no
  `toArray`, so the port reads the list through `size` and iteration and
  stays self-consistent. The port already rejects more than 16 rules from an
  ordinary list, and already copies the caller's list; both were verified
  against `java.time`.

### Fixed

- **`TCKZoneIdPrinterParser`, 80 failures, now 0.** Two separate bugs, both
  in that one class.

  First, `parseUnresolved` built an accessor whose offset query only read
  `OFFSET_SECONDS`. Java answers the
  offset query from the parsed zone when that zone is itself a `ZoneOffset`,
  and never sets `OFFSET_SECONDS` while doing it. The resolved accessor
  already had that rule; the unresolved one did not. Guarded by
  `DateTimeFormatterUnresolvedZoneQueryJavaConformanceTest`.

  Second, zone ids were parsed by longest match over `ZoneId.of`, which is
  more permissive than the parser Java uses. Java parses structurally: a
  `UTC`/`UT`/`GMT` prefix, then an offset needing two-digit hours *and*
  minutes, with seconds optional. When that offset is malformed the prefix
  stands alone and the rest of the text is left unparsed, so `UTC-01` stops
  at index 3 with zone `UTC`, where the port consumed `-01` and answered
  `UTC-01:00`. A bare `+01` is a parse failure in Java and was accepted by the
  port. Guarded by `DateTimeFormatterZoneIdParseJavaConformanceTest`, which
  compares index, error index, and zone across 43 inputs in both
  case-sensitive and case-insensitive mode.

- **`getTransitions` and `getTransitionRules` returned mutable lists,
  3 failures.** Java returns unmodifiable lists. Kotlin's read-only `List` is
  a compile-time guarantee only, and `map` and `listOf` hand back an
  `ArrayList`, so a Java caller could clear them. `getTransitionRules`
  returned the rules list itself, so clearing it corrupted the shared
  `ZoneRules` for the rest of the process — writing the regression test
  against the unfixed code broke two unrelated tests that used the same zone.
  Both now return a read-only view. Guarded by
  `ZoneRulesListImmutabilityJavaConformanceTest`.

- **`TCKPadPrinterParser.test_parseStrict`, 1 failure.** A strict parse must
  fill the whole padded width, and Java reports a short parse at the start of
  the padded section plus the index the padding ended at, counting the start
  twice. The port reported it at the start. The double count is only visible
  when something precedes the padding, so the test puts a literal in front.
  Guarded by `DateTimeFormatterPadParseJavaConformanceTest`.

- **`TestZonedDateTime.test_duration`, 1 failure.** `until` moved the end of
  the difference into the start's zone, which pushes the local date-time past
  the end of the timeline when the start is already at `LocalDateTime.MAX`.
  Java catches that and moves the start into the end's zone instead. Guarded
  by `ZonedDateTimeTimelineEndsJavaConformanceTest`, which compares `until`
  for every `ChronoUnit` and `Duration.between` over 75 zone and value pairs.

### Found once the converter reached more files

The static-import and Locale rules brought 30 more files in, and they
immediately exposed differences nothing had tested before. All three were
reproduced outside the harness, against `java.time`, before being recorded.

- **Numeric overflow while parsing, 9 failures in `TestNumberParser`.** Java's
  value parser stops taking digits when the next one would overflow a `long`,
  and succeeds with what it has: `appendValue(DAY_OF_MONTH, 1, 19, NORMAL)`
  parsing `9223372036854775808` returns index 18 and the value
  922337203685477580. The port rejects the whole parse and reports an error at
  index 0. Same for `99999999999999999999` and the negative side.
- **Adjacent value parse errors, 3 failures in `TestNumberParser`.** With a
  value field followed by a fixed-width one, Java reports a failure after the
  first field has been consumed; parsing `"1"` gives error index 1. The port
  reports index 0. The success case, `"12"`, already matches.
- **`TestCharLiteralPrinter.test_toString_apos`, 1 failure.** The builder's
  `toString` escapes an apostrophe literal as `''''` where Java prints `''`.

Two more are harness artifacts: `TestZoneId.test_systemDefault_*` set the JVM
default time zone and expect the port's exception type, but the JVM's own
`java.time` throws first.

### Left open — a design call, not a defect

- **`TestLocalTime` whole-hour singletons, 5 failures.** `assertSame` checks.
  Java caches the 24 whole-hour `LocalTime` values and returns them from the
  factories; the port allocates. This is an allocation strategy, asserted by
  OpenJDK's internal `test` tree rather than by the compatibility `tck` tree,
  so matching it is a design call rather than a compatibility fix.

## Not yet converted (42 files)

42 files are dropped. The census below predates the static-import and Locale
rules; re-measure it with `-Xmaxerrs 10000` before working the list.

Formatting was the largest blind spot before those rules landed, 46 of 73: `TCKDateTimeFormatter`,
`TCKDateTimeFormatterBuilder`, `TCKDateTimeFormatters`, `TCKDateTimeParseResolver`
and 11 more from the `tck` tree. Then chronology, 14: every calendar
`TCKChronology` except `TCKIsoChronology`, which the `resolveDate` fix
unblocked and which added 445 passing tests. Also lost: `TCKLocalDate`, `TCKZoneId`, `TCKZoneOffset`,
`TCKDayOfWeek`, `TCKMonth`, `TCKZoneRulesProvider`.

The census below was re-measured against the JDK 25 TCK and the current port,
after the covariant `resolveDate` fix, with `-Xmaxerrs 10000`.

Every earlier count of "100 errors over 22 files" in this file was wrong. 100
is `javac`'s default error cap, so the measurement was truncated and read as a
round number. The real figure is **477 errors over 65 files**. Always pass
`-Xmaxerrs` when censusing this.

Causes, largest first:

- `java.util.Locale` versus the port's own `Locale` (214, 45% of the total) —
  needs a bridge at the call sites, not a change to the port
- method references (174). `LocalDate::from` and friends. The converter
  rewrites `Type.member(` call syntax but not `Type::member`, which needs
  `Type.Companion::member`. This is the largest converter gap and was hidden
  behind the error cap.
- the noun-accessor rule over-applying (37). `.length()` is rewritten to
  `.getLength()` even on a `String`; the rule needs to know the receiver type.
- static imports of `object` members (`IsoFields.QUARTER_OF_YEAR`) and of
  companion constants (`DateTimeFormatter.BASIC_ISO_DATE`) — 34
- `toFormat()` (15). A JVM extension function on `DateTimeFormatter`, so it is
  a static on a file class and invisible to a Java caller. Real interop note,
  not a converter gap.
- `getAvailableZoneIds()` and `getAvailableChronologies()` (24) — check the
  port's names against `java.time` before assuming either side is wrong.
- covariant return types (6). Checked against `java.time` with `javap`:
  `eras()` is more specific than Java's. Java returns `List<Era>`; the port
  returns `List<HijrahEra>` and so on. Kotlin's `List` is covariant so this
  reads better from Kotlin, but Java's is invariant, so a Java caller cannot
  assign the result to `List<Era>`. Every use in the TCK is read-only, so the
  harness can declare `List<? extends Era>` and keep the port's shape.
  `resolveDate` was the other half of this and is now fixed in the port.
- `datesUntil` returns `Sequence`, not `Stream` — permanent exclusion
- `jdk.test.lib` jtreg infrastructure — exclude

## Next steps

1. Fix the three real differences the wider conversion exposed: numeric
   overflow while parsing, the adjacent-value error index, and the apostrophe
   in the builder's `toString`. The first is the most serious.
2. Re-census the 42 dropped files with `-Xmaxerrs 10000`. The old census is
   stale and the largest cause on it, `Locale`, is now handled.
3. Teach the converter method references: `LocalDate::from` needs
   `LocalDate.Companion::from`. That was 174 errors before the Locale rule and
   is likely the largest remaining gap.
4. Add `-Xmaxerrs` to the script's own compile so the census is never
   truncated again.
5. Add the `eras()` rule: rewrite the declared `List<Era>` to
   `List<? extends Era>`. Every use in the TCK is read-only, so this keeps the
   port's shape and unblocks `tck/chrono`.
6. The failure count will not reach zero: 21 of the 29 cannot be fixed in the
   port. To make the CI job blocking, have the script compare against a
   checked-in baseline of expected failures and fail on anything new, rather
   than waiting for a clean run. The baseline should carry the dropped-file
   list too, so lost coverage cannot grow unnoticed.
