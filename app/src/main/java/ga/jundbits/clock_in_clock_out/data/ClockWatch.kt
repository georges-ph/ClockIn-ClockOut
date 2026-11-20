package ga.jundbits.clock_in_clock_out.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.edit
import kotlin.math.abs

/**
 * Notices when the phone's clock is changed between two records.
 *
 * The wall clock can be set by hand, but the time since boot
 * (`elapsedRealtime`) can't. Their difference stays the same until someone
 * changes the clock, so a jump in it is the size of the change. After a
 * reboot the time since boot starts again, so the reference is reset and a
 * change made while the phone was off can't be seen.
 */
object ClockWatch {

    /** Network time corrections are a few seconds; anything larger was set by someone. */
    const val THRESHOLD_MS = 2L * 60 * 1000

    private const val PREFS = "clock_watch"
    private const val KEY_OFFSET = "offset"
    private const val KEY_BOOT = "boot"

    /**
     * How far the clock moved since the reference, or 0 when it didn't move,
     * the phone restarted, or there is no reference yet.
     */
    fun shift(referenceOffset: Long?, referenceBoot: Int?, offset: Long, boot: Int): Long {
        if (referenceOffset == null || referenceBoot == null || referenceBoot != boot || boot < 0) return 0
        val moved = offset - referenceOffset
        return if (abs(moved) >= THRESHOLD_MS) moved else 0
    }

    /**
     * Starts measuring when the app opens, if there is nothing to compare with yet
     * (first run, or the phone restarted). A reference from this boot is kept, so
     * a change waiting to be recorded isn't lost.
     */
    fun start(context: Context) {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val boot = currentBoot(context)
        if (preferences.contains(KEY_OFFSET) && preferences.getInt(KEY_BOOT, -1) == boot) return
        preferences.edit {
            putLong(KEY_OFFSET, currentOffset())
            putInt(KEY_BOOT, boot)
        }
    }

    private fun currentOffset() = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    private fun currentBoot(context: Context) =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)

    /** How far the clock has moved since the last record, without resetting anything. */
    fun pending(context: Context): Long {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return shift(
            referenceOffset = if (preferences.contains(KEY_OFFSET)) preferences.getLong(KEY_OFFSET, 0) else null,
            referenceBoot = if (preferences.contains(KEY_BOOT)) preferences.getInt(KEY_BOOT, -1) else null,
            offset = currentOffset(),
            boot = currentBoot(context),
        )
    }

    /** Returns how far the clock moved since the last record, and starts measuring again from now. */
    fun consume(context: Context): Long {
        val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val offset = currentOffset()
        val boot = currentBoot(context)
        val moved = shift(
            referenceOffset = if (preferences.contains(KEY_OFFSET)) preferences.getLong(KEY_OFFSET, 0) else null,
            referenceBoot = if (preferences.contains(KEY_BOOT)) preferences.getInt(KEY_BOOT, -1) else null,
            offset = offset,
            boot = boot,
        )
        preferences.edit {
            putLong(KEY_OFFSET, offset)
            putInt(KEY_BOOT, boot)
        }
        return moved
    }
}
