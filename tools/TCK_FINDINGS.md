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
Total tests run: 17648, Passes: 17618, Failures: 30
```

82 of 155 TCK/test files ran; 73 were dropped because the converter cannot
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

### Worth investigating — possible real divergence

- **`TestZonedDateTime.test_duration`.** `Invalid value for EpochDay ...
  365241780472` at the end of the range.
- **`TestLocalTime` whole-hour singletons, 5 failures.** `assertSame` checks.
  Java caches the 24 whole-hour `LocalTime` values and returns them from the
  factories; the port allocates. This is an allocation strategy, asserted by
  OpenJDK's internal `test` tree rather than by the compatibility `tck` tree,
  so matching it is a design call rather than a compatibility fix.
- **`TestMutableZoneRules.testLength`.** Expected `IllegalArgumentException`,
  nothing thrown.

## Not yet converted (73 files)

73 files are dropped. The error census below was measured against the JDK 21
TCK, before the harness moved to JDK 25; re-measure it before working items 2
and 3 under Next steps. Known causes:

- `java.util.Locale` versus the port's own `Locale` (28) — needs an adapter
  or per-site rewrite
- covariant return types: `ChronoLocalDate` where the TCK expects
  `HijrahDate`/`ThaiBuddhistDate` (28), `List<Era>` versus `List<JapaneseEra>`
  (6) — worth checking against Java before assuming it is a converter gap
- static imports of `object` members (`IsoFields.QUARTER_OF_YEAR`) and of
  companion constants (`DateTimeFormatter.BASIC_ISO_DATE`) — converter gap
- `datesUntil` returns `Sequence`, not `Stream` — permanent exclusion
- `jdk.test.lib` jtreg infrastructure — exclude

## Next steps

1. The compatibility `tck` tree is clean. Its 6 remaining failures are the
   5 `test_serialization` and the Coptic `ServiceLoader` fixture, neither of
   which is a port defect. The other 24 come from OpenJDK's internal `test`
   tree: 14 `test_immutable`, 1 Coptic `ServiceLoader`, 2 `TestClock_System`,
   and 7 real ones, of which 5 are the `LocalTime` whole-hour cache.
2. Add static-import rules to the converter. Cheap, unlocks several files.
3. Decide on `Locale` and the covariant return types.
4. Make the CI job blocking once the failure count reaches zero.
