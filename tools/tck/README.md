# Stored TCK patch

`tck.patch` is applied by `tools/run-tck.main.kts` to the OpenJDK `java.time`
TCK after that script has fetched it and renamed `java.time` to
`io.heapy.krogu.time`. It carries the hand-maintained part of the conversion.

## Licence

**This patch is derived from OpenJDK test sources that are GPLv2 only, with no
Classpath exception.** It was generated against `openjdk/jdk25u` at tag
`jdk-25.0.4.1-ga`, and it therefore is governed by GPLv2 rather than by the
Apache-2.0 licence that covers the rest of this repository.

Nothing else here is GPL-derived. The TCK itself is never committed: the script
fetches it into `build/tck/` at run time. The patch is kept as small as the
design allows, with one line of context on each side, and the script refuses to
store a hunk that adds or deletes a whole file, because such a hunk would carry
a complete GPLv2-only source into an Apache-2.0 repository.

## What is in the patch, and what is not

The split is by what an edit tracks.

Edits that track **the port** stay as rules in `tools/run-tck.main.kts`, so a
change to the port's Java-facing surface needs no edit here. Those are the
package rename, the `Companion` and `INSTANCE` receivers that stand in for
missing JVM statics, the Kotlin-property accessors from `MIGRATION_NOTES.md`,
and the bridge from `java.util.Locale` to the port's own `Locale`. All but the
accessor list are read out of the built jar.

Edits that track **the pinned TCK tag** live in this patch. They are shaped by
one frozen revision of someone else's source, so a regex over it would silently
stop matching when the tag moves, where a patch fails loudly and names the
files. Today that is the `List<Era>` to `List<? extends Era>` widening that
Kotlin's covariant read-only lists need when a Java caller looks at them
through an invariant `List`: 11 hunks across 9 files.

The patch is also where a test is disabled when one has to be. The TCK runs
under TestNG, so the annotation is `@Test(enabled = false)`, not JUnit's
`@Disabled`. No test is disabled today.

## Regenerating

    KOTLIN_CLI_NO_WELCOME_BANNER=1 ./kotlin build
    kotlinr tools/run-tck.main.kts
    # edit build/tck/gen by hand
    kotlinr tools/run-tck.main.kts --capture-patch

The first run writes the mechanically converted tree to `build/tck/base` and a
patched copy of it to `build/tck/gen`. `build/tck/gen` is the tree to edit;
`build/tck/base` is the reference the capture diffs against, and
`build/tck/src` is a throwaway copy that `javac` prunes, so the files it cannot
compile stay editable in `build/tck/gen`.

`--capture-patch` re-derives `build/tck/base`, diffs it against
`build/tck/gen`, replays the result onto a fresh copy of the base, and refuses
to write the patch unless that replay reproduces `build/tck/gen` byte for byte.

## When the pinned tag moves

`tools/run-tck.main.kts` records the tag in the patch header and compares it
against its own `tckTag` before applying anything. A mismatch, or a hunk that
no longer applies, stops the run and names what failed; it never continues with
a half-patched tree. Recovery is the regeneration sequence above: the run
leaves `build/tck/gen` holding the unpatched tree, so the edits can be redone
there and captured again.
