#!/usr/bin/env kotlinr

/**
 * Runs the OpenJDK java.time TCK against krogu-time.
 *
 * The TCK sources are GPLv2 only, with no Classpath exception. They are
 * fetched into the work directory and never committed, so the only
 * GPL-derived text this Apache-2.0 project carries is the small stored patch
 * described in tools/tck/README.md.
 *
 * Conversion runs in two layers, split by what each edit tracks.
 *
 * The mechanical layer holds the edits that track the port, so that a change
 * to the port's Java-facing surface needs no edit here. Its rules are read
 * out of the built jar: companion and object members without JVM static
 * bridges are reached through Companion or INSTANCE, and the parameter
 * positions that take the port's own Locale come from the same dump.
 *
 * The stored patch holds the edits that track the pinned TCK tag instead.
 * Those are shaped by one frozen revision of someone else's source, so a
 * line-oriented patch expresses them honestly and breaks loudly when the tag
 * moves, where a regex would silently stop matching.
 *
 * Usage:
 *   kotlinr tools/run-tck.main.kts [work-dir]
 *   kotlinr tools/run-tck.main.kts --capture-patch [work-dir]
 *
 * A normal run converts into <work>/base, copies that to <work>/gen, applies
 * the stored patch to <work>/gen, then compiles and runs a pruned copy. To
 * change what the patch does: run normally once, edit <work>/gen by hand,
 * then run again with --capture-patch to store the difference back into
 * tools/tck/tck.patch. Tests that should not run are disabled the TestNG
 * way, with @Test(enabled = false); none are disabled today.
 *
 * Requires `./kotlin build` first, and the reference JDK on JAVA_HOME or in
 * the Kotlin Toolchain cache.
 */

import java.io.File
import java.net.URI
import java.util.zip.ZipFile
import kotlin.system.exitProcess

// Must match the JDK that runs the JVM differential tests, because that JDK
// is the behavioral reference of this port (module.yaml, jvm.jdk.version).
// The TCK checks SHORT_IDS, tzdb rules, and field ranges that move between
// JDK releases, so a mismatch here compares against the wrong java.time.
val jdkMajor = "25"
val tckTag = "jdk-25.0.4.1-ga"
val tckRepository = "https://github.com/openjdk/jdk${jdkMajor}u.git"

val newPackage = "io.heapy.krogu.time"

// Serialization tests are excluded permanently: the port implements no
// java.io.Serializable, by design on a Kotlin Multiplatform target.
val excludedPaths = listOf("/serial/", "Serialization")

val libraries = listOf(
    "org/testng/testng/7.10.2/testng-7.10.2.jar",
    "com/beust/jcommander/1.82/jcommander-1.82.jar",
    "org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar",
    "org/jetbrains/kotlin/kotlin-stdlib/2.4.0/kotlin-stdlib-2.4.0.jar",
    "org/jetbrains/annotations/13.0/annotations-13.0.jar",
)

val root = File(".").absoluteFile.normalize()

val options = args.filter { it.startsWith("--") }
val positional = args.filter { !it.startsWith("--") }
val capturePatch = "--capture-patch" in options

val work = File(positional.getOrNull(0) ?: "build/tck").let {
    if (it.isAbsolute) it else File(root, it.path)
}

// The tree the mechanical rules produce, the tree the stored patch is
// applied to and a human edits, and the throwaway copy javac prunes.
val base = File(work, "base")
val generated = File(work, "gen")
val compiled = File(work, "src")
val patchFile = File(root, "tools/tck/tck.patch")

fun run(
    vararg command: String,
    dir: File = root,
    mergeError: Boolean = true,
): Pair<Int, String> {
    val process = ProcessBuilder(*command)
        .directory(dir)
        .redirectErrorStream(mergeError)
        .apply { if (!mergeError) redirectError(ProcessBuilder.Redirect.INHERIT) }
        .start()
    val output = process.inputStream.bufferedReader().readText()
    return process.waitFor() to output
}

fun ensure(condition: Boolean, message: () -> String) {
    if (!condition) {
        System.err.println(message())
        exitProcess(1)
    }
}

ensure(options.all { it == "--capture-patch" }) {
    "unknown option: ${options.first { it != "--capture-patch" }}\n" +
        "usage: kotlinr tools/run-tck.main.kts [--capture-patch] [work-dir]"
}

// --- reference JDK ------------------------------------------------------

fun isReferenceJdk(home: File): Boolean {
    val javac = File(home, "bin/javac")
    if (!javac.canExecute()) return false
    return run(javac.path, "-version").second.contains(" $jdkMajor.")
}

val toolchainCache = File(System.getProperty("user.home"), "Library/Caches/JetBrains/Kotlin/extract.cache")
val jdkCandidates = buildList {
    System.getenv("JAVA_HOME")?.let { add(File(it)) }
    run("/usr/libexec/java_home", "-v", jdkMajor).let { (code, out) ->
        if (code == 0) add(File(out.trim()))
    }
    if (toolchainCache.isDirectory) {
        toolchainCache.walkTopDown().maxDepth(4)
            .filter { it.isDirectory && it.name == "Home" && it.path.contains("$jdkMajor.0") }
            .forEach { add(it) }
    }
}
val jdkOrNull = jdkCandidates.firstOrNull(::isReferenceJdk)
ensure(jdkOrNull != null) { "JDK $jdkMajor not found. Set JAVA_HOME to a JDK $jdkMajor." }
val jdk = jdkOrNull!!
val javac = File(jdk, "bin/javac").path
val javap = File(jdk, "bin/javap").path
val java = File(jdk, "bin/java").path
println("JDK: ${run(javac, "-version").second.trim()}")

// --- classpath -----------------------------------------------------------

val portJarOrNull = File(root, "build/tasks").walkTopDown()
    .firstOrNull { it.name.endsWith("-jvm.jar") && it.path.contains("jarJvm") }
ensure(portJarOrNull != null) { "no built jar found, run ./kotlin build first" }
val portJar = portJarOrNull!!

File(work, "lib").mkdirs()
libraries.forEach { path ->
    val target = File(work, "lib/${path.substringAfterLast('/')}")
    if (!target.exists()) {
        println("downloading ${target.name}")
        URI("https://repo1.maven.org/maven2/$path").toURL().openStream().use { input ->
            target.outputStream().use(input::copyTo)
        }
    }
}
val classpath = (listOf(portJar.path) + File(work, "lib").listFiles()!!.map { it.path })
    .joinToString(File.pathSeparator)

// --- TCK sources ---------------------------------------------------------

val checkout = File(work, "jdk-tck")
if (!checkout.isDirectory) {
    println("fetching TCK at $tckTag")
    run(
        "git", "clone", "--filter=blob:none", "--no-checkout", "--depth", "1",
        "--branch", tckTag, tckRepository, checkout.path,
    )
    run("git", "-C", checkout.path, "sparse-checkout", "init", "--cone")
    run("git", "-C", checkout.path, "sparse-checkout", "set", "test/jdk/java/time")
    val (code, out) = run("git", "-C", checkout.path, "checkout")
    ensure(code == 0) { "sparse checkout failed:\n$out" }
}
val sources = File(checkout, "test/jdk/java/time")
ensure(sources.isDirectory) { "TCK sources missing at ${sources.path}" }

// --- learn the rewrite rules from the jar --------------------------------

class Members(val methods: MutableSet<String> = mutableSetOf(), val properties: MutableSet<String> = mutableSetOf())

data class JvmSurface(
    val instanceMethods: Map<String, Set<String>>,
    val staticMethods: Set<String>,
    val staticFields: Set<String>,
)

val surfaceCache = mutableMapOf<String, JvmSurface>()

fun surfaceOf(className: String): JvmSurface = surfaceCache.getOrPut(className) {
    val instanceMethods = mutableMapOf<String, MutableSet<String>>()
    val staticMethods = mutableSetOf<String>()
    val staticFields = mutableSetOf<String>()
    val lines = run(javap, "-s", "-cp", portJar.path, className).second.lines()
    lines.forEachIndexed { index, line ->
        val method = Regex("""^\s*public .+\b([\w$]+)\(.*\);$""").find(line)
        if (method != null) {
            val name = method.groupValues[1]
            val descriptor = Regex("""^\s*descriptor: (.+)$""")
                .find(lines.getOrNull(index + 1).orEmpty())?.groupValues?.get(1)
                ?: return@forEachIndexed
            val signature = "$name$descriptor"
            if (Regex("""\bstatic\b""").containsMatchIn(line)) {
                staticMethods += signature
            } else {
                instanceMethods.getOrPut(name) { mutableSetOf() } += signature
            }
            return@forEachIndexed
        }
        Regex("""^\s*public static .+ ([\w$]+);$""").find(line)?.let { field ->
            staticFields += field.groupValues[1]
        }
    }
    JvmSurface(instanceMethods, staticMethods, staticFields)
}

fun membersOf(className: String, staticOwner: String = className): Members {
    val members = Members()
    val surface = surfaceOf(className)
    val ownerSurface = surfaceOf(staticOwner)
    surface.instanceMethods.forEach { (name, signatures) ->
        if (name == "Companion") return@forEach
        val property = Regex("""^get([A-Z_]\w*)$""").find(name)?.groupValues?.get(1)
        if (property != null) {
            val hasStatic = property in ownerSurface.staticFields ||
                signatures.all { it in ownerSurface.staticMethods }
            if (!hasStatic) members.properties += property
        } else if (signatures.any { it !in ownerSurface.staticMethods }) {
            members.methods += name
        }
    }
    return members
}

val classNames = ZipFile(portJar).use { jar ->
    jar.entries().asSequence()
        .filter { it.name.endsWith(".class") }
        .map { it.name.removeSuffix(".class").replace('/', '.') }
        .toList()
}
val classNameSet = classNames.toSet()

// Companion members are static-looking in Java source but are instance
// members of the Companion object on the JVM.
val companions = mutableMapOf<String, Members>()
classNames.filter { it.endsWith("\$Companion") }.forEach { companion ->
    val owner = companion.removeSuffix("\$Companion")
    val simple = owner.substringAfterLast('.').substringAfterLast('$')
    val members = membersOf(companion, owner)
    val existing = companions.getOrPut(simple) { Members() }
    existing.methods += members.methods
    existing.properties += members.properties
}

// Kotlin object declarations expose their members through INSTANCE.
val singletons = mutableMapOf<String, Members>()
classNames.filter { '$' !in it && "${it}\$Companion" !in classNameSet }.forEach { name ->
    val dump = run(javap, "-cp", portJar.path, name).second
    if (!Regex("""public static final \S+ INSTANCE;""").containsMatchIn(dump)) return@forEach
    singletons[name.substringAfterLast('.')] = membersOf(name)
}

// Enum constants are real statics and must never be rewritten.
val enumConstants = mutableMapOf<String, Set<String>>()
classNames.filter { '$' !in it }.forEach { name ->
    val simple = name.substringAfterLast('.')
    if (simple !in companions) return@forEach
    val dump = run(javap, "-cp", portJar.path, name).second
    enumConstants[simple] = Regex("""public static final \S+ (\w+);""")
        .findAll(dump).map { it.groupValues[1] }.toSet()
}

// Accessors that read as nouns became Kotlin properties (MIGRATION_NOTES.md).
val nounAccessors = listOf(
    "range", "length", "dayOfWeek", "weekOfMonth", "weekOfYear",
    "weekOfWeekBasedYear", "weekBasedYear",
)

fun splitParameters(parameters: String): List<String> {
    if (parameters.isBlank()) return emptyList()
    val result = mutableListOf<String>()
    var start = 0
    var genericDepth = 0
    parameters.forEachIndexed { index, char ->
        when (char) {
            '<' -> genericDepth++
            '>' -> genericDepth--
            ',' -> if (genericDepth == 0) {
                result += parameters.substring(start, index).trim()
                start = index + 1
            }
        }
    }
    result += parameters.substring(start).trim()
    return result
}

// Locale positions come from the same JVM surface as the companion and
// singleton rules, so new locale-taking port APIs are covered automatically.
val localeParameters = mutableMapOf<String, MutableSet<Int>>()
val (localeDumpCode, localeDump) = run(
    javap, "-cp", portJar.path, *classNames.toTypedArray(),
)
ensure(localeDumpCode == 0) { "javap failed while reading locale parameters:\n$localeDump" }
localeDump.lineSequence().forEach { line ->
    val signature = Regex("""\b([\w$]+)\((.*)\);$""").find(line)
        ?: return@forEach
    val method = signature.groupValues[1]
    splitParameters(signature.groupValues[2]).forEachIndexed { index, parameter ->
        if (parameter == "$newPackage.Locale") {
            localeParameters.getOrPut(method) { mutableSetOf() } += index
        }
    }
}

// --- rewrite -------------------------------------------------------------

// Never rewrite inside string literals: the TCK asserts on text that
// contains "java.time", and those expectations must stay as they are.
fun rewritePackages(text: String): String =
    text.replace(Regex("""("(?:[^"\\]|\\.)*")|\bjava\.time\b""")) { match ->
        if (match.groupValues[1].isNotEmpty()) match.value else newPackage
    }

data class SourceReplacement(val start: Int, val end: Int, val value: String)

fun applyReplacements(text: String, replacements: List<SourceReplacement>): String {
    val result = StringBuilder(text)
    replacements.sortedByDescending { it.start }.forEach { replacement ->
        result.replace(replacement.start, replacement.end, replacement.value)
    }
    return result.toString()
}

// Masking comments and literals lets token offsets stay aligned with the
// source while ensuring their contents can never become rewrite targets.
fun codeMask(text: String): String {
    val mask = text.toCharArray()
    var index = 0
    while (index < text.length) {
        val end = when {
            text.startsWith("//", index) -> {
                text.indexOf('\n', index).let { if (it == -1) text.length else it }
            }
            text.startsWith("/*", index) -> {
                text.indexOf("*/", index + 2).let { if (it == -1) text.length else it + 2 }
            }
            text[index] == '"' || text[index] == '\'' -> {
                val quote = text[index]
                var cursor = index + 1
                while (cursor < text.length) {
                    if (text[cursor] == '\\') cursor++
                    else if (text[cursor] == quote) {
                        cursor++
                        break
                    }
                    cursor++
                }
                cursor
            }
            else -> {
                index++
                continue
            }
        }
        for (masked in index until end) {
            if (mask[masked] != '\n' && mask[masked] != '\r') mask[masked] = ' '
        }
        index = end
    }
    return mask.concatToString()
}

data class CallArguments(val ranges: List<IntRange>)

fun callArguments(text: String, open: Int): CallArguments? {
    val ranges = mutableListOf<IntRange>()
    var start = open + 1
    var parentheses = 0
    var brackets = 0
    var braces = 0
    var index = start
    while (index < text.length) {
        when {
            text.startsWith("//", index) -> {
                index = text.indexOf('\n', index).let { if (it == -1) text.length else it }
                continue
            }
            text.startsWith("/*", index) -> {
                index = text.indexOf("*/", index + 2)
                    .let { if (it == -1) text.length else it + 2 }
                continue
            }
            text[index] == '"' || text[index] == '\'' -> {
                val quote = text[index++]
                while (index < text.length) {
                    if (text[index] == '\\') index++
                    else if (text[index] == quote) {
                        index++
                        break
                    }
                    index++
                }
                continue
            }
        }
        when (text[index]) {
            '(' -> parentheses++
            ')' -> if (parentheses == 0) {
                if (text.substring(start, index).isNotBlank()) {
                    ranges += start until index
                }
                return CallArguments(ranges)
            } else parentheses--
            '[' -> brackets++
            ']' -> brackets--
            '{' -> braces++
            '}' -> braces--
            ',' -> if (parentheses == 0 && brackets == 0 && braces == 0) {
                ranges += start until index
                start = index + 1
            }
        }
        index++
    }
    return null
}

fun receiverBefore(text: String, dot: Int): String? {
    var end = dot
    while (end > 0 && text[end - 1].isWhitespace()) end--
    var start = end
    while (start > 0 && (text[start - 1].isJavaIdentifierPart() || text[start - 1] == '$')) {
        start--
    }
    var receiver = text.substring(start, end)
    if (receiver != "Companion" && receiver != "INSTANCE") return receiver.ifEmpty { null }
    var ownerEnd = start
    while (ownerEnd > 0 && text[ownerEnd - 1].isWhitespace()) ownerEnd--
    if (ownerEnd == 0 || text[ownerEnd - 1] != '.') return receiver
    ownerEnd--
    while (ownerEnd > 0 && text[ownerEnd - 1].isWhitespace()) ownerEnd--
    var ownerStart = ownerEnd
    while (ownerStart > 0 && text[ownerStart - 1].isJavaIdentifierPart()) ownerStart--
    receiver = text.substring(ownerStart, ownerEnd)
    return receiver.ifEmpty { null }
}

var rewrites = 0

fun rewriteStaticImports(source: String): String {
    val imported = mutableMapOf<String, String>()
    val staticImport = Regex(
        """^import static (${Regex.escape(newPackage)}\.[\w.]+)\.(\w+);""",
        RegexOption.MULTILINE,
    )
    var text = source.replace(staticImport) { match ->
        val owner = match.groupValues[1]
        val simple = owner.substringAfterLast('.')
        val member = match.groupValues[2]
        val replacement = when {
            member in (enumConstants[simple] ?: emptySet()) -> null
            member in (singletons[simple]?.properties ?: emptySet()) ->
                "$owner.INSTANCE.get$member()"
            member in (singletons[simple]?.methods ?: emptySet()) ->
                "$owner.INSTANCE.$member"
            member in (companions[simple]?.properties ?: emptySet()) ->
                "$owner.Companion.get$member()"
            member in (companions[simple]?.methods ?: emptySet()) ->
                "$owner.Companion.$member"
            else -> null
        }
        if (replacement == null) match.value else {
            imported[member] = replacement
            rewrites++
            ""
        }
    }
    val mask = codeMask(text)
    val replacements = imported.flatMap { (member, replacement) ->
        Regex("""(?<![.\w])${Regex.escape(member)}\b""").findAll(mask).map { match ->
            rewrites++
            SourceReplacement(match.range.first, match.range.last + 1, replacement)
        }.toList()
    }
    text = applyReplacements(text, replacements)
    return text
}

// A Java method reference still needs the explicit Kotlin object receiver
// because the port deliberately provides no static bridge.
fun rewriteMethodReferences(source: String, foreignTypes: Set<String>): String {
    val mask = codeMask(source)
    val replacements = mutableListOf<SourceReplacement>()

    fun collect(owner: String, member: String, receiver: String) {
        if (member in (enumConstants[owner] ?: emptySet())) return
        val reference = Regex(
            """(?<![.\w])${Regex.escape(owner)}(?=\s*::\s*${Regex.escape(member)}\b)""",
        )
        reference.findAll(mask).forEach { match ->
            replacements += SourceReplacement(
                match.range.last + 1,
                match.range.last + 1,
                receiver,
            )
            rewrites++
        }
    }

    companions.forEach { (simple, members) ->
        if (simple !in foreignTypes) {
            members.methods.forEach { collect(simple, it, ".Companion") }
        }
    }
    singletons.forEach { (simple, members) ->
        if (simple !in foreignTypes) {
            members.methods.forEach { collect(simple, it, ".INSTANCE") }
        }
    }
    return applyReplacements(source, replacements)
}

fun isJavaLocale(expression: String, names: Set<String>): Boolean {
    val value = expression.trim()
    if (Regex("""^\(\s*(?:java\.util\.)?Locale\s*\)""").containsMatchIn(value)) {
        return true
    }
    if (Regex("""^(?:java\.util\.)?Locale\b""").containsMatchIn(value)) return true
    val leadingName = Regex("""^\(*\s*([A-Za-z_$][\w$]*)""")
        .find(value)?.groupValues?.get(1)
    return leadingName in names
}

fun rewriteJavaLocales(
    source: String,
    foreignTypes: Set<String>,
    foreignValues: Set<String>,
    inheritedLocaleNames: Set<String>,
): String {
    val javaLocaleNames = if ("Locale" in foreignTypes) {
        Regex("""(?<![\w.])Locale\s*(?:\[\s*])?\s*(?:\.\.\.)?\s*(\w+)""")
            .findAll(codeMask(source)).map { it.groupValues[1] }.toSet()
    } else emptySet()
    val importedLocaleNames = Regex(
        """^import static java\.util\.Locale\.(\w+);""",
        RegexOption.MULTILINE,
    ).findAll(source).map { it.groupValues[1] }.toSet()
    val streamedLocaleNames = Regex(
        """Arrays\.stream\(\s*Locale\.getAvailableLocales\(\)\s*\)\s*""" +
            """\.forEach\(\s*(\w+)\s*->""",
    ).findAll(codeMask(source)).map { it.groupValues[1] }.toSet()
    val localeNames = javaLocaleNames + importedLocaleNames + inheritedLocaleNames +
        streamedLocaleNames
    if ("Locale" !in foreignTypes && localeNames.isEmpty()) return source

    val mask = codeMask(source)
    val replacements = mutableListOf<SourceReplacement>()
    Regex("""\.\s*([A-Za-z_$][\w$]*)\s*\(""").findAll(mask).forEach { call ->
        val method = call.groupValues[1]
        val localePositions = localeParameters[method] ?: return@forEach
        val dot = call.range.first
        val receiver = receiverBefore(mask, dot)
        if (receiver in foreignTypes || receiver in foreignValues) return@forEach
        if (method == "of" && receiver !in companions && receiver !in singletons) {
            return@forEach
        }
        val open = mask.indexOf('(', call.range.first)
        val arguments = callArguments(source, open) ?: return@forEach
        localePositions.forEach { position ->
            val range = arguments.ranges.getOrNull(position) ?: return@forEach
            val start = range.first + source.substring(range).indexOfFirst { !it.isWhitespace() }
            val end = range.last + 1 - source.substring(range).reversed()
                .indexOfFirst { !it.isWhitespace() }
            val expression = source.substring(start, end)
            if (!isJavaLocale(expression, localeNames)) return@forEach
            val bridge = "$newPackage.Locale.Companion.forLanguageTag(" +
                "($expression).toLanguageTag())"
            replacements += SourceReplacement(start, end, bridge)
            rewrites++
        }
    }
    return applyReplacements(source, replacements)
}

base.deleteRecursively()
base.mkdirs()

// Only the tck and test trees. The sibling nontestng tree is jtreg-only
// scaffolding with no TestNG classes.
val tckFiles = listOf("tck", "test")
    .map { File(sources, it) }
    .flatMap { it.walkTopDown() }
    .filter { it.isFile && it.extension == "java" }
    .filter { file -> excludedPaths.none { it in file.path } }
    .toList()

data class JavaLocaleScope(
    val names: Set<String>,
    val superclass: String?,
)

val javaLocaleScopes = tckFiles.mapNotNull { file ->
    val source = file.readText()
    val mask = codeMask(source)
    val className = Regex("""\bclass\s+(\w+)""").find(mask)?.groupValues?.get(1)
        ?: return@mapNotNull null
    val importsJavaLocale = Regex(
        """^import java\.util\.Locale;""",
        RegexOption.MULTILINE,
    ).containsMatchIn(source)
    val names = if (importsJavaLocale) {
        Regex("""(?<![\w.])Locale\s*(?:\[\s*])?\s*(?:\.\.\.)?\s*(\w+)""")
            .findAll(mask).map { it.groupValues[1] }.toSet()
    } else emptySet()
    val superclass = Regex("""\bclass\s+\w+\s+extends\s+(\w+)""")
        .find(mask)?.groupValues?.get(1)
    className to JavaLocaleScope(names, superclass)
}.toMap()

fun inheritedLocaleNames(file: File): Set<String> {
    var className = Regex("""\bclass\s+(\w+)""")
        .find(codeMask(file.readText()))?.groupValues?.get(1)
    val names = mutableSetOf<String>()
    val visited = mutableSetOf<String>()
    while (className != null && visited.add(className)) {
        val scope = javaLocaleScopes[className] ?: break
        names += scope.names
        className = scope.superclass
    }
    return names
}

tckFiles.forEach { file ->
    var text = rewritePackages(file.readText())

    // A simple name imported from outside the port is not a port type.
    val foreign = Regex("""^import\s+(?!io\.heapy\.krogu\.)[\w.]*\.(\w+);""", RegexOption.MULTILINE)
        .findAll(text).map { it.groupValues[1] }.toSet()
    val masked = codeMask(text)
    val foreignValues = foreign.flatMap { type ->
        Regex("""(?<![\w.])${Regex.escape(type)}\s+(\w+)""")
            .findAll(masked).map { it.groupValues[1] }.toList()
    }.toSet()

    text = rewriteStaticImports(text)
    text = rewriteMethodReferences(text, foreign)

    companions.forEach { (simple, members) ->
        if (simple in foreign) return@forEach
        members.methods.forEach { method ->
            text = text.replace(Regex("""(?<![.\w])$simple\.$method\s*\(""")) {
                rewrites++; "$simple.Companion.$method("
            }
        }
        members.properties.forEach { property ->
            if (property in (enumConstants[simple] ?: emptySet())) return@forEach
            text = text.replace(Regex("""(?<![.\w])$simple\.$property\b(?!\s*\()""")) {
                rewrites++; "$simple.Companion.get$property()"
            }
        }
    }

    singletons.forEach { (simple, members) ->
        if (simple in foreign) return@forEach
        members.methods.forEach { method ->
            text = text.replace(Regex("""(?<![.\w])$simple\.$method\s*\(""")) {
                rewrites++; "$simple.INSTANCE.$method("
            }
        }
        members.properties.forEach { property ->
            text = text.replace(Regex("""(?<![.\w])$simple\.$property\b(?!\s*\()""")) {
                rewrites++; "$simple.INSTANCE.get$property()"
            }
        }
    }

    nounAccessors.forEach { noun ->
        val getter = "get" + noun.replaceFirstChar(Char::uppercase)
        text = text.replace(Regex("""\.$noun\(\)""")) { rewrites++; ".$getter()" }
        text = text.replace(Regex("""\b(public|protected)\s+(\S+)\s+$noun\(\)""")) {
            rewrites++
            "${it.groupValues[1]} ${it.groupValues[2]} $getter()"
        }
    }

    text = rewriteJavaLocales(text, foreign, foreignValues, inheritedLocaleNames(file))

    val relative = file.relativeTo(sources).path
        .replace("java/time", newPackage.replace('.', '/'))
    val target = File(base, relative)
    target.parentFile.mkdirs()
    target.writeText(text)
}
println("converted ${tckFiles.size} files, $rewrites rewrites")

// --- stored patch --------------------------------------------------------

// The tag lives in the patch header because a line-oriented patch is only
// meaningful against the revision it was cut from. patch(1) treats anything
// before the first diff header as a comment, so the header costs nothing.
val patchTagPrefix = "generated against:"

fun storedPatchTag(): String? = patchFile.readLines()
    .takeWhile { it.startsWith("#") || it.isBlank() }
    .firstNotNullOfOrNull {
        Regex("""^#\s*$patchTagPrefix\s*(\S+)\s*$""").find(it)?.groupValues?.get(1)
    }

fun copyTree(from: File, to: File) {
    to.deleteRecursively()
    ensure(from.copyRecursively(to, overwrite = true)) { "could not copy ${from.path}" }
}

fun treeFiles(tree: File): Map<String, File> = tree.walkTopDown()
    .filter { it.isFile }
    .associateBy { it.relativeTo(tree).invariantSeparatorsPath }

fun treeDifference(left: File, right: File): List<String> {
    val a = treeFiles(left)
    val b = treeFiles(right)
    return (a.keys + b.keys).sorted().filter { path ->
        val one = a[path]?.readBytes()
        val other = b[path]?.readBytes()
        one == null || other == null || !one.contentEquals(other)
    }
}

// patch(1) rather than `git apply`: the work directory sits inside this
// repository but is ignored by it, and there `git apply` reports success
// without touching a single file. A silent no-op is exactly the failure this
// design exists to make loud, so the portable tool wins.
//
// -F0 because patch defaults to a fuzz factor of 2, which with one line of
// context means it will happily place a hunk by line number alone after the
// surrounding source has changed underneath it. Demanding an exact context
// match is the whole reason the tag is pinned.
fun applyPatch(tree: File, patch: File, dryRun: Boolean): Pair<Int, String> = run(
    "patch", "-p2", "-F0", "--batch",
    *(if (dryRun) arrayOf("--dry-run") else emptyArray()),
    "-i", patch.path,
    dir = tree,
)

// BSD patch names the file it gave up on; GNU patch names the reject file it
// would have written. Reading both keeps the message useful on either.
fun failingPatchFiles(output: String): List<String> = listOf(
    Regex("""hunks? failed while patching '?(\S+?)'?$""", RegexOption.MULTILINE),
    Regex("""saving rejects to file (\S+)\.rej$""", RegexOption.MULTILINE),
).flatMap { pattern ->
    pattern.findAll(output).map { it.groupValues[1] }
}.distinct()

val captureCommand = "kotlinr tools/run-tck.main.kts --capture-patch"

if (capturePatch) {
    ensure(generated.isDirectory) {
        "no converted tree at ${generated.path}. Run the script without " +
            "--capture-patch first, then edit that tree by hand."
    }
    // Relative arguments keep the strip count at -p2 no matter where the
    // work directory lives.
    val (diffCode, diff) = run(
        "git", "diff", "--no-index", "--no-color", "-U1",
        base.name, generated.name,
        dir = work, mergeError = false,
    )
    ensure(diffCode == 0 || diffCode == 1) {
        "git diff failed while capturing the patch"
    }

    // Whole files must never reach the repository: they are GPLv2-only
    // sources, and a deletion hunk carries the entire file as context.
    val wholeFile = Regex(
        """^diff --git a/\S+ b/(\S+)$\n(?:^(?!@@|diff ).*$\n)*?""" +
            """^(?:new|deleted) file mode""",
        setOf(RegexOption.MULTILINE),
    ).findAll(diff).map { it.groupValues[1] }.toList()
    ensure(wholeFile.isEmpty()) {
        "refusing to store a patch that adds or removes whole files, because " +
            "such a hunk carries a complete GPLv2-only source into this " +
            "repository:\n" + wholeFile.joinToString("\n") { "  $it" }
    }

    val candidate = File(work, "tck.patch.candidate")
    candidate.writeText(
        """
        |# krogu-time TCK patch
        |# $patchTagPrefix $tckTag
        |#
        |# Derived from OpenJDK test sources that are GPLv2 only, with no
        |# Classpath exception, and therefore governed by GPLv2 rather than by
        |# this repository's Apache-2.0 licence. See tools/tck/README.md.
        |#
        |# Regenerate by editing build/tck/gen by hand and running
        |#   $captureCommand
        |
        |
        """.trimMargin() + diff,
    )

    // The captured patch is only trustworthy if replaying it onto a fresh
    // base reproduces the edited tree byte for byte.
    val trial = File(work, "patch-check")
    copyTree(base, trial)
    if (diff.isNotBlank()) {
        val (code, output) = applyPatch(trial, candidate, dryRun = false)
        ensure(code == 0) {
            "the captured patch does not replay onto ${base.name}:\n$output"
        }
    }
    val mismatch = treeDifference(trial, generated)
    ensure(mismatch.isEmpty()) {
        "the captured patch does not reproduce ${generated.path}:\n" +
            mismatch.joinToString("\n") { "  $it" }
    }
    trial.deleteRecursively()

    patchFile.parentFile.mkdirs()
    candidate.copyTo(patchFile, overwrite = true)
    candidate.delete()
    val hunks = diff.lines().count { it.startsWith("@@") }
    val touched = diff.lines().count { it.startsWith("diff --git") }
    println(
        "captured ${patchFile.relativeTo(root).invariantSeparatorsPath}: " +
            "$hunks hunks across $touched files, " +
            "${patchFile.readLines().size} lines",
    )
    exitProcess(0)
}

copyTree(base, generated)
if (!patchFile.isFile) {
    println(
        "no stored patch at ${patchFile.relativeTo(root).invariantSeparatorsPath}, " +
            "running the mechanically converted tree unchanged",
    )
} else {
    val recordedTag = storedPatchTag()
    ensure(recordedTag != null) {
        "${patchFile.path} carries no '# $patchTagPrefix <tag>' header, so " +
            "there is no way to tell which TCK revision it was cut from. " +
            "Regenerate it with $captureCommand."
    }
    ensure(recordedTag == tckTag) {
        "the stored patch was generated against $recordedTag but this script " +
            "pins $tckTag. A line-oriented patch does not survive a tag move. " +
            "Run the script once to get a converted tree at ${generated.path}, " +
            "redo the edits there by hand, then run $captureCommand."
    }
    val hunks = patchFile.readLines().count { it.startsWith("@@") }
    val touched = patchFile.readLines().count { it.startsWith("diff --git") }
    if (hunks == 0) {
        println("stored patch is empty, nothing to apply")
    } else {
        val (checkCode, checkOutput) = applyPatch(generated, patchFile, dryRun = true)
        ensure(checkCode == 0) {
            val failing = failingPatchFiles(checkOutput)
            "the stored patch no longer applies to the TCK at $tckTag.\n" +
                (if (failing.isEmpty()) checkOutput
                else "Files that did not apply:\n" +
                    failing.joinToString("\n") { "  $it" }) +
                "\n${generated.path} holds the unpatched tree. Redo those edits " +
                "there by hand, then run $captureCommand."
        }
        val (applyCode, applyOutput) = applyPatch(generated, patchFile, dryRun = false)
        ensure(applyCode == 0) {
            "the stored patch passed its dry run and then failed to " +
                "apply:\n$applyOutput"
        }
        println("applied stored patch: $hunks hunks across $touched files")
    }
}

// javac prunes the files it cannot compile, so it works on a copy: the tree
// a human edits and the capture step diffs must keep every file.
copyTree(generated, compiled)

// --- compile -------------------------------------------------------------

val classes = File(work, "classes")
classes.deleteRecursively()
classes.mkdirs()

// Files the converter cannot handle yet are dropped, not silently ignored:
// each one is reported so lost coverage stays visible.
val dropped = mutableSetOf<String>()
var log = ""
for (round in 1..8) {
    val remaining = compiled.walkTopDown().filter { it.extension == "java" }.map { it.path }.toList()
    if (remaining.isEmpty()) break
    val (code, output) = run(
        javac, "-nowarn", "-Xmaxerrs", "10000", "-d", classes.path,
        "-cp", classpath, "-sourcepath", ".",
        *remaining.toTypedArray(), dir = compiled,
    )
    log = output
    if (code == 0) break
    val failing = Regex("""^(\S+\.java):""", RegexOption.MULTILINE).findAll(output)
        .map { it.groupValues[1] }.toSet()
    ensure(failing.isNotEmpty()) { "javac failed without naming a file:\n$output" }
    failing.forEach { path ->
        val file = File(path).let { if (it.isAbsolute) it else File(compiled, path) }
        dropped += file.relativeTo(compiled).path
        file.delete()
    }
}
val standing = compiled.walkTopDown().filter { it.extension == "java" }.toList()
ensure(standing.isNotEmpty()) { "nothing compiled:\n$log" }
println("converted files still standing: ${standing.size}")
println("dropped files (no TCK coverage): ${dropped.size}")
dropped.sorted().forEach { println("  $it") }

// --- run -----------------------------------------------------------------

val testClasses = standing
    .map { it.relativeTo(compiled).path.removeSuffix(".java").replace('/', '.') }
    .filter { Regex("""\.(TCK|Test)[A-Za-z_]*$""").containsMatchIn(it) }
    .filterNot { "Abstract" in it }
    .sorted()

val (testCode, testOutput) = run(
    java, "-cp", "${classes.path}${File.pathSeparator}$classpath",
    "org.testng.TestNG", "-testclass", testClasses.joinToString(","),
    "-d", File(work, "report").path,
)
println(testOutput.lineSequence().filter { "Total tests run" in it }.joinToString("\n"))

// The job reports divergence; it does not gate the build while the
// converter still drops files.
if (testCode != 0) println("TCK reported failures, see ${File(work, "report").path}")
