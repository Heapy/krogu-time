package io.heapy.krogu.time.format

import io.heapy.krogu.time.temporal.TemporalAccessor
import io.heapy.krogu.time.temporal.TemporalQuery
import java.text.FieldPosition
import java.text.Format
import java.text.ParseException
import java.text.ParsePosition

/** Returns this formatter as a classic Java text [Format]. */
public fun DateTimeFormatter.toFormat(): Format = ClassicFormat(this, null)

/** Returns a classic Java text [Format] that converts parsed values with [parseQuery]. */
public fun DateTimeFormatter.toFormat(parseQuery: TemporalQuery<*>): Format =
    ClassicFormat(this, parseQuery)

private class ClassicFormat(
    private val formatter: DateTimeFormatter,
    private val parseQuery: TemporalQuery<*>?,
) : Format() {
    override fun format(
        obj: Any,
        toAppendTo: StringBuffer,
        pos: FieldPosition,
    ): StringBuffer {
        if (obj !is TemporalAccessor) {
            throw IllegalArgumentException("Format target must implement TemporalAccessor")
        }
        pos.beginIndex = 0
        pos.endIndex = 0
        try {
            formatter.formatTo(obj, toAppendTo)
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException(exception.message, exception)
        }
        return toAppendTo
    }

    override fun parseObject(source: String): Any? = try {
        val position = ParsePosition(0)
        val parsed = formatter.parse(source, position)
        if (position.index < source.length) {
            throw DateTimeParseException(
                "Text could not be parsed, unparsed text found at index ${position.index}",
                source,
                position.index,
            )
        }
        applyQuery(parsed)
    } catch (exception: DateTimeParseException) {
        throw ParseException(exception.message, exception.errorIndex).apply {
            initCause(exception)
        }
    } catch (exception: RuntimeException) {
        throw ParseException(exception.message, 0).apply {
            initCause(exception)
        }
    }

    override fun parseObject(
        source: String,
        pos: ParsePosition,
    ): Any? {
        val previousErrorIndex = pos.errorIndex
        pos.errorIndex = -1
        return try {
            val parsed = formatter.parse(source, pos)
            pos.errorIndex = previousErrorIndex
            applyQuery(parsed)
        } catch (_: RuntimeException) {
            if (pos.errorIndex < 0) {
                pos.errorIndex = 0
            }
            null
        }
    }

    private fun applyQuery(parsed: TemporalAccessor): Any? =
        if (parseQuery == null) parsed else parseQuery.queryFrom(parsed)
}
