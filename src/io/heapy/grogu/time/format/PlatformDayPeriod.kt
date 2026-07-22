package io.heapy.grogu.time.format

internal data class LocaleDayPeriod(
    val text: String,
    val fromMinute: Int,
    val toMinute: Int,
) {
    fun includes(minuteOfDay: Long): Boolean =
        fromMinute == 0 && toMinute == 0 && minuteOfDay == MINUTES_PER_DAY.toLong() ||
            fromMinute == minuteOfDay.toInt() && toMinute == minuteOfDay.toInt() ||
            fromMinute <= minuteOfDay && minuteOfDay < toMinute ||
            fromMinute > toMinute && (fromMinute <= minuteOfDay || toMinute > minuteOfDay)

    fun midpoint(): Int {
        val duration = if (fromMinute > toMinute) {
            MINUTES_PER_DAY - fromMinute + toMinute
        } else {
            toMinute - fromMinute
        }
        return (fromMinute + duration / 2) % MINUTES_PER_DAY
    }
}

internal expect fun formatLocaleDayPeriod(
    languageTag: String,
    hour: Int,
    minute: Int,
    style: TextStyle,
): String

internal expect fun localeDayPeriods(
    languageTag: String,
    style: TextStyle,
): List<LocaleDayPeriod>

internal fun discoverLocaleDayPeriods(
    textAt: (hour: Int, minute: Int) -> String,
): List<LocaleDayPeriod> {
    val hourlyText = (0 until 24).map { hour -> textAt(hour, 1) }
    val ranges = mutableListOf<LocaleDayPeriod>()
    var rangeStartHour = 0
    for (hour in 1..24) {
        if (hour == 24 || hourlyText[hour] != hourlyText[rangeStartHour]) {
            ranges += LocaleDayPeriod(
                text = hourlyText[rangeStartHour],
                fromMinute = rangeStartHour * 60,
                toMinute = hour * 60,
            )
            rangeStartHour = hour
        }
    }
    if (ranges.size > 1 && ranges.first().text == ranges.last().text) {
        val first = ranges.removeFirst()
        val last = ranges.removeLast()
        ranges += LocaleDayPeriod(last.text, last.fromMinute, first.toMinute)
    }
    val exactPeriods = (0 until 24).mapNotNull { hour ->
        val exactText = textAt(hour, 0)
        exactText.takeIf { it != hourlyText[hour] }?.let { text ->
            LocaleDayPeriod(text, hour * 60, hour * 60)
        }
    }
    return (exactPeriods + ranges).distinct()
}

private const val MINUTES_PER_DAY: Int = 24 * 60
