package ga.jundbits.clock_in_clock_out.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeTest {

    @Test
    fun roundTrips() {
        val profile = Profile("007", "Ann Lee", "IT")
        assertEquals(BadgeData("007", "Ann Lee", "IT"), Badge.decode(Badge.encode(profile)))
    }

    @Test
    fun readsCodesFromVersion2() {
        val old = """{"id":36512666,"name":"Liam Warren","department":"IT","clocking":"IN"}"""
        assertEquals(BadgeData("36512666", "Liam Warren", "IT"), Badge.decode(old))
    }

    @Test
    fun readsAnIdOnlyCode() = assertEquals(BadgeData("A-1001", "", ""), Badge.decode(" A-1001 "))

    @Test
    fun rejectsOtherCodes() {
        assertNull(Badge.decode("https://example.com/menu"))
        assertNull(Badge.decode("""{"name":"No ID"}"""))
        assertNull(Badge.decode("{not json"))
    }
}
