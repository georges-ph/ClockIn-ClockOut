package ga.jundbits.clock_in_clock_out.data

/** Decides whether a scan is a clock-in or a clock-out, so employees carry one badge. */
object ClockRule {

    /** A clock-in older than this counts as a forgotten clock-out, so the next scan is an IN again. */
    const val OPEN_SHIFT_MS = 16L * 60 * 60 * 1000

    /** A second scan this soon after the last one is probably the same person scanned twice. */
    const val RECENT_MS = 2L * 60 * 1000

    fun next(last: Attendance?, now: Long): Clocking {
        if (last == null || last.clocking == Clocking.OUT) return Clocking.IN
        val age = now - last.timestamp
        return if (age in 0 until OPEN_SHIFT_MS) Clocking.OUT else Clocking.IN
    }

    fun isRecent(last: Attendance?, now: Long): Boolean =
        last != null && now - last.timestamp in 0 until RECENT_MS
}
