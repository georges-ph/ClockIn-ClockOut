package ga.jundbits.clock_in_clock_out.ui

import android.content.Context
import android.text.format.DateFormat
import ga.jundbits.clock_in_clock_out.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formats {

    /** Follows the phone's 12 or 24 hour setting. */
    fun time(context: Context, timestamp: Long): String =
        DateFormat.getTimeFormat(context).format(Date(timestamp))

    fun startOfDay(timestamp: Long): Long = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun daysAgo(timestamp: Long, now: Long): Long {
        val day = 24L * 60 * 60 * 1000
        return Math.round((startOfDay(now) - startOfDay(timestamp)).toDouble() / day)
    }

    private fun shortDate(timestamp: Long): String =
        SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM"), Locale.getDefault()).format(Date(timestamp))

    /** A list heading: "Today", "Yesterday" or a short date. */
    fun dayLabel(context: Context, timestamp: Long, now: Long = System.currentTimeMillis()): String =
        when (daysAgo(timestamp, now)) {
            0L -> context.getString(R.string.today)
            1L -> context.getString(R.string.yesterday)
            else -> shortDate(timestamp)
        }

    /** Inline wording such as "today 08:02" or "yesterday 17:31". */
    fun whenText(context: Context, timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val time = time(context, timestamp)
        return when (daysAgo(timestamp, now)) {
            0L -> context.getString(R.string.when_today, time)
            1L -> context.getString(R.string.when_yesterday, time)
            else -> context.getString(R.string.when_date, shortDate(timestamp), time)
        }
    }

    /** "Phone clock moved back 1 h 2 min before this", for a record whose clock was changed. */
    fun clockShift(context: Context, shift: Long): String =
        context.getString(if (shift < 0) R.string.clock_moved_back else R.string.clock_moved_forward, duration(context, shift))

    /** The notice shown when the gate phone opens after its time was changed. */
    fun clockChangedBody(context: Context, shift: Long): String =
        context.getString(if (shift < 0) R.string.clock_changed_back_body else R.string.clock_changed_forward_body, duration(context, shift))

    /** "1 h 2 min" or "5 min", ignoring the sign. */
    private fun duration(context: Context, millis: Long): String {
        val minutes = Math.round(Math.abs(millis) / 60_000.0).toInt()
        return if (minutes >= 60) context.getString(R.string.duration_hours, minutes / 60, minutes % 60)
        else context.getString(R.string.duration_minutes, minutes)
    }

    fun initials(name: String): String =
        name.split(' ', '-').filter { it.isNotBlank() }.let { parts ->
            when {
                parts.isEmpty() -> "?"
                parts.size == 1 -> parts[0].take(2)
                else -> "${parts.first().first()}${parts.last().first()}"
            }
        }.uppercase(Locale.getDefault())
}
