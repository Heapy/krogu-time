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
Total tests run: 17648, Passes: 17590, Failures: 58
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

On the full tree that is 7645 companion calls, 1285 companion constants,
283 object calls, 221 noun accessors, 2104 package references.

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

- **`TCKZoneIdPrinterParser`, 80 failures, now 24.** `parseUnresolved` built
  an accessor whose offset query only read `OFFSET_SECONDS`. Java answers the
  offset query from the parsed zone when that zone is itself a `ZoneOffset`,
  and never sets `OFFSET_SECONDS` while doing it. The resolved accessor
  already had that rule; the unresolved one did not. Guarded by
  `DateTimeFormatterUnresolvedZoneQueryJavaConformanceTest`.

### Worth investigating — possible real divergence

- **`getTransitions_immutable` / `getTransitionRules_immutable`, 3 failures.**
  Java returns a list that throws on mutation. The port's list does not.
- **`TestZonedDateTime.test_duration`.** `Invalid value for EpochDay ...
  365241780472` at the end of the range.
- **`TestLocalTime.factory_ofNanoOfDay_singletons`.** An `assertSame` check.
  Java caches whole-hour `LocalTime` constants; the port makes new instances.
- **`TCKPadPrinterParser.test_parseStrict`.** `expected [1] but found [0]`.
- **`TestMutableZoneRules.testLength`.** Expected `IllegalArgumentException`,
  nothing thrown.

## Not yet converted (74 files)

100 compile errors over 22 files, cascading to 74 dropped. Known causes:

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

1. Work through the remaining `TCKZoneIdPrinterParser` failures (24 left).
2. Add static-import rules to the converter. Cheap, unlocks several files.
3. Decide on `Locale` and the covariant return types.
4. Make the CI job blocking once the failure count reaches zero.
