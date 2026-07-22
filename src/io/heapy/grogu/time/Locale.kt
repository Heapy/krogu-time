package io.heapy.grogu.time

/**
 * A locale identifier used by locale-sensitive date-time APIs.
 *
 * Grogu-time represents locales as canonical BCP 47 language tags so the
 * same value can be used on every Kotlin Multiplatform target.
 */
public class Locale private constructor(
    private val canonicalLanguageTag: String,
) {
    /** The ISO language subtag, or an empty string for [ROOT]. */
    public val language: String
        get() = canonicalLanguageTag.substringBefore('-').takeUnless { it == UNDEFINED_LANGUAGE } ?: ""

    /** The ISO 15924 script subtag, or an empty string when absent. */
    public val script: String
        get() = subtags().firstOrNull(::isScriptSubtag).orEmpty()

    /** The region subtag, or an empty string when absent. */
    public val country: String
        get() = subtags().firstOrNull(::isRegionSubtag).orEmpty()

    /** Returns this locale as a canonical BCP 47 language tag. */
    public fun toLanguageTag(): String = canonicalLanguageTag

    /** Returns the type associated with the two-character Unicode locale [key]. */
    public fun getUnicodeLocaleType(key: String): String? {
        require(key.length == 2 && key.all(Char::isAsciiLetterOrDigit)) {
            "Ill-formed Unicode locale key: $key"
        }
        val requestedKey = key.asciiLowercase()
        val subtags = canonicalLanguageTag.split('-')
        val unicodeExtensionIndex = subtags.indexOfFirst { it == "u" }
        val privateUseIndex = subtags.indexOfFirst { it == "x" }
        if (
            unicodeExtensionIndex < 0 ||
            privateUseIndex >= 0 && privateUseIndex < unicodeExtensionIndex
        ) {
            return null
        }

        var index = unicodeExtensionIndex + 1
        while (index < subtags.size && subtags[index].length != 1) {
            val current = subtags[index]
            if (current.length != 2) {
                index++
                continue
            }
            val typeStart = index + 1
            var typeEnd = typeStart
            while (
                typeEnd < subtags.size &&
                subtags[typeEnd].length != 1 &&
                subtags[typeEnd].length != 2
            ) {
                typeEnd++
            }
            if (current == requestedKey) {
                return subtags.subList(typeStart, typeEnd).joinToString("-")
            }
            index = typeEnd
        }
        return null
    }

    override fun equals(other: Any?): Boolean =
        this === other || other is Locale && canonicalLanguageTag == other.canonicalLanguageTag

    override fun hashCode(): Int = canonicalLanguageTag.hashCode()

    override fun toString(): String = canonicalLanguageTag

    private fun subtags(): List<String> = canonicalLanguageTag.split('-').drop(1).takeWhile { it.length != 1 }

    public companion object {
        private const val UNDEFINED_LANGUAGE: String = "und"

        /** The language-neutral root locale. */
        public val ROOT: Locale = Locale(UNDEFINED_LANGUAGE)

        /** The English language locale. */
        public val ENGLISH: Locale = Locale("en")

        /** English as used in the United States. */
        public val US: Locale = Locale("en-US")

        /** English as used in the United Kingdom. */
        public val UK: Locale = Locale("en-GB")

        /** Creates a locale from a BCP 47 [languageTag]. */
        public fun forLanguageTag(languageTag: String): Locale {
            val canonical = canonicalizeLanguageTag(languageTag)
            return when (canonical) {
                UNDEFINED_LANGUAGE -> ROOT
                "en" -> ENGLISH
                "en-US" -> US
                "en-GB" -> UK
                else -> Locale(canonical)
            }
        }

        /** Returns the platform's current default locale for formatting. */
        public fun getDefault(): Locale = forLanguageTag(defaultFormatLocaleTag())
    }
}

private fun canonicalizeLanguageTag(languageTag: String): String {
    val subtags = languageTag
        .trim()
        .split('-')
        .filter(String::isNotEmpty)
    if (subtags.isEmpty()) return "und"
    if (!subtags.first().isAsciiLetters() || subtags.first().length !in 2..8) return "und"

    var scriptSeen = false
    var regionSeen = false
    var extension = false
    return subtags.mapIndexed { index, subtag ->
        when {
            index == 0 -> subtag.asciiLowercase()
            subtag.length == 1 -> {
                extension = true
                subtag.asciiLowercase()
            }
            extension -> subtag.asciiLowercase()
            !scriptSeen && isScriptSubtag(subtag) -> {
                scriptSeen = true
                subtag.asciiTitlecase()
            }
            !regionSeen && isRegionSubtag(subtag) -> {
                regionSeen = true
                subtag.asciiUppercase()
            }
            else -> subtag.asciiLowercase()
        }
    }.joinToString("-")
}

private fun isScriptSubtag(value: String): Boolean = value.length == 4 && value.isAsciiLetters()

private fun isRegionSubtag(value: String): Boolean =
    (value.length == 2 && value.isAsciiLetters()) ||
        (value.length == 3 && value.all(Char::isDigit))

private fun String.isAsciiLetters(): Boolean = all { it in 'A'..'Z' || it in 'a'..'z' }

private fun Char.isAsciiLetterOrDigit(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9'

private fun String.asciiLowercase(): String = map { character ->
    if (character in 'A'..'Z') character + ('a' - 'A') else character
}.joinToString("")

private fun String.asciiUppercase(): String = map { character ->
    if (character in 'a'..'z') character - ('a' - 'A') else character
}.joinToString("")

private fun String.asciiTitlecase(): String =
    asciiLowercase().replaceFirstChar { character ->
        if (character in 'a'..'z') character - ('a' - 'A') else character
    }
