package ga.jundbits.clock_in_clock_out.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ClockWatchTest {

    private val minute = 60_000L
    private val offset = 1_800_000_000_000L

    @Test
    fun noReferenceYet() = assertEquals(0L, ClockWatch.shift(null, null, offset, 3))

    @Test
    fun smallCorrectionsAreIgnored() = assertEquals(0L, ClockWatch.shift(offset, 3, offset + 30_000, 3))

    @Test
    fun clockSetBackIsNegative() = assertEquals(-62 * minute, ClockWatch.shift(offset, 3, offset - 62 * minute, 3))

    @Test
    fun clockSetForwardIsPositive() = assertEquals(10 * minute, ClockWatch.shift(offset, 3, offset + 10 * minute, 3))

    @Test
    fun aRestartResetsTheReference() = assertEquals(0L, ClockWatch.shift(offset, 3, offset + 999 * minute, 4))
}
