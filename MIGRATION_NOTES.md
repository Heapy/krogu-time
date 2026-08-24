# Migration notes

`krogu-time` mirrors the Java 21 `java.time` API, so converting Java code is
mostly mechanical. The IDE Java-to-Kotlin converter handles the common cases,
including JavaBean getters such as `getYear()`, which become the `year`
property.

The cases below are the ones that need a manual change.

## Accessors that became properties

Java methods that already read as nouns are exposed as Kotlin properties, so the
call parentheses have to go. The converter keeps the parentheses and the result
does not compile.

| Java | Kotlin |
| --- | --- |
| `year.length()` | `year.length` |
| `field.range()` | `field.range` |
| `weekFields.dayOfWeek()` | `weekFields.dayOfWeek` |
| `weekFields.weekOfMonth()` | `weekFields.weekOfMonth` |
| `weekFields.weekOfYear()` | `weekFields.weekOfYear` |
| `weekFields.weekOfWeekBasedYear()` | `weekFields.weekOfWeekBasedYear` |
| `weekFields.weekBasedYear()` | `weekFields.weekBasedYear` |

## Null arguments

Java accepts `null` for a few query arguments and answers instead of failing.
Those parameters are nullable here too, and they keep the Java answer:

- `isSupported(null)` returns `false` on every temporal type.
- `ZoneRules` query methods accept a `null` instant or local date-time. The Java
  contract is conditional and is reproduced exactly: rules with a single offset
  ignore the value and return a result, while rules with transitions reject it
  the way Java does.

## `datesUntil` returns a `Sequence`

`LocalDate.datesUntil` returns a Kotlin `Sequence`, the idiomatic equivalent of
Java's `Stream`. The two differ in how they count:

| | Result type | `MIN..MAX` |
| --- | --- | --- |
| Java `Stream.count()` | `Long` | 730 484 999 633, returned immediately |
| Kotlin `Sequence.count()` | `Int` | does not fit, and walks every element |

A `Stream` knows its size in advance, while a `Sequence` counts by iterating.
So translate

```kotlin
start.datesUntil(end).count()   // Int, walks the whole range
```

into

```kotlin
start.until(end, ChronoUnit.DAYS)   // Long, computed directly
```

Small ranges behave the same either way; only very large ones overflow `Int` or
take a long time.

The other `Stream` operations have `Sequence` equivalents under different names,
such as `collect(Collectors.toList())` to `toList()` and `allMatch { }` to
`all { }`.
