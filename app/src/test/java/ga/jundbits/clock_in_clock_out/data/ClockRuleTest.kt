package ga.jundbits.clock_in_clock_out.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockRuleTest {

    private val hour = 60L * 60 * 1000
    private val now = 1_800_000_000_000L

    private fun record(clocking: Clocking, ago: Long) =
        Attendance(employeeId = "1", employeeName = "A", employeeDepartment = "B", clocking = clocking, timestamp = now - ago)

    @Test
    fun firstScanIsIn() = assertEquals(Clocking.IN, ClockRule.next(null, now))

    @Test
    fun afterOutComesIn() = assertEquals(Clocking.IN, ClockRule.next(record(Clocking.OUT, 2 * hour), now))

    @Test
    fun afterRecentInComesOut() = assertEquals(Clocking.OUT, ClockRule.next(record(Clocking.IN, 8 * hour), now))

    @Test
    fun nightShiftAcrossMidnightStillClocksOut() = assertEquals(Clocking.OUT, ClockRule.next(record(Clocking.IN, 15 * hour), now))

    @Test
    fun forgottenClockOutStartsANewDayWithIn() = assertEquals(Clocking.IN, ClockRule.next(record(Clocking.IN, 20 * hour), now))

    @Test
    fun clockSetBackwardsCountsAsNewShift() = assertEquals(Clocking.IN, ClockRule.next(record(Clocking.IN, -hour), now))

    @Test
    fun recentScanIsFlagged() {
        assertTrue(ClockRule.isRecent(record(Clocking.IN, 30_000), now))
        assertFalse(ClockRule.isRecent(record(Clocking.IN, 5 * 60_000), now))
        assertFalse(ClockRule.isRecent(null, now))
    }
}
