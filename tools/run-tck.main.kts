#!/usr/bin/env kotlinr

/**
 * Runs the OpenJDK java.time TCK against krogu-time.
 *
 * The TCK sources are GPLv2 only, with no Classpath exception. They are
 * fetched into the work directory and never committed, so this Apache-2.0
 * project redistributes nothing GPL-licensed.
 *
 * The port declares no @JvmStatic, so every companion member is reached
 * through Companion. The rewrite rules for that are read out of the built
 * jar rather than written by hand, so they follow the port automatically.
 *
 * Usage:
 *   kotlinr tools/run-tck.main.kts [work-dir]
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
val work = File(args.getOrNull(0) ?: "build/tck").let {
    if (it.isAbsolute) it else File(root, it.path)
}

fun run(vararg command: String, dir: File = root): Pair<Int, String> {
    val process = ProcessBuilder(*command)
        .directory(dir)
        .redirectErrorStream(true)
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

fun membersOf(className: String): Members {
    val members = Members()
    run(javap, "-cp", portJar.path, className).second.lineSequence().forEach { line ->
        if ("class " in line || "static" in line) return@forEach
        val name = Regex("""\b(\w+)\(""").find(line)?.groupValues?.get(1) ?: return@forEach
        if (name == "Companion") return@forEach
        val property = Regex("""^get([A-Z_]\w*)$""").find(name)?.groupValues?.get(1)
        if (property != null) members.properties += property else members.methods += name
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
    val members = membersOf(companion)
    // javap prints Companion members without the static modifier, so the
    // filter in membersOf keeps them; take them as they are.
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

// --- rewrite -------------------------------------------------------------

// Never rewrite inside string literals: the TCK asserts on text that
// contains "java.time", and those expectations must stay as they are.
fun rewritePackages(text: String): String =
    text.replace(Regex("""("(?:[^"\\]|\\.)*")|\bjava\.time\b""")) { match ->
        if (match.groupValues[1].isNotEmpty()) match.value else newPackage
    }

val generated = File(work, "gen")
generated.deleteRecursively()
generated.mkdirs()

// Only the tck and test trees. The sibling nontestng tree is jtreg-only
// scaffolding with no TestNG classes.
val tckFiles = listOf("tck", "test")
    .map { File(sources, it) }
    .flatMap { it.walkTopDown() }
    .filter { it.isFile && it.extension == "java" }
    .filter { file -> excludedPaths.none { it in file.path } }
    .toList()

var rewrites = 0
tckFiles.forEach { file ->
    var text = rewritePackages(file.readText())

    // A simple name imported from outside the port is not a port type.
    val foreign = Regex("""^import\s+(?!io\.heapy\.krogu\.)[\w.]*\.(\w+);""", RegexOption.MULTILINE)
        .findAll(text).map { it.groupValues[1] }.toSet()

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

    val relative = file.relativeTo(sources).path
        .replace("java/time", newPackage.replace('.', '/'))
    val target = File(generated, relative)
    target.parentFile.mkdirs()
    target.writeText(text)
}
println("converted ${tckFiles.size} files, $rewrites rewrites")

// --- compile -------------------------------------------------------------

val classes = File(work, "classes")
classes.deleteRecursively()
classes.mkdirs()

// Files the converter cannot handle yet are dropped, not silently ignored:
// each one is reported so lost coverage stays visible.
val dropped = mutableSetOf<String>()
var log = ""
for (round in 1..8) {
    val remaining = generated.walkTopDown().filter { it.extension == "java" }.map { it.path }.toList()
    if (remaining.isEmpty()) break
    val (code, output) = run(
        javac, "-nowarn", "-d", classes.path, "-cp", classpath, "-sourcepath", ".",
        *remaining.toTypedArray(), dir = generated,
    )
    log = output
    if (code == 0) break
    val failing = Regex("""^(\S+\.java):""", RegexOption.MULTILINE).findAll(output)
        .map { it.groupValues[1] }.toSet()
    ensure(failing.isNotEmpty()) { "javac failed without naming a file:\n$output" }
    failing.forEach { path ->
        val file = File(path).let { if (it.isAbsolute) it else File(generated, path) }
        dropped += file.relativeTo(generated).path
        file.delete()
    }
}
val standing = generated.walkTopDown().filter { it.extension == "java" }.toList()
ensure(standing.isNotEmpty()) { "nothing compiled:\n$log" }
println("converted files still standing: ${standing.size}")
println("dropped files (no TCK coverage): ${dropped.size}")
dropped.sorted().forEach { println("  $it") }

// --- run -----------------------------------------------------------------

val testClasses = standing
    .map { it.relativeTo(generated).path.removeSuffix(".java").replace('/', '.') }
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
