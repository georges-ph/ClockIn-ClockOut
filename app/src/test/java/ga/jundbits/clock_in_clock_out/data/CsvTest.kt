package ga.jundbits.clock_in_clock_out.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class CsvTest {

    private fun parsed(text: String) = Csv.parseProfiles(text) as ImportResult.Parsed

    @Test
    fun readsTheReadmeExample() {
        val result = parsed("id,name,department\n80811897,Victoria Bennett,Customer Support\n33660096,Grace Stevens,Engineering\n")
        assertEquals(listOf(Profile("80811897", "Victoria Bennett", "Customer Support"), Profile("33660096", "Grace Stevens", "Engineering")), result.profiles)
        assertTrue(result.skipped.isEmpty())
    }

    @Test
    fun acceptsExcelUtf8WithCapitalHeadersAndCrlf() {
        val result = parsed(Csv.BOM + "ID,Name,Department\r\n007,Ann Lee,IT\r\n")
        assertEquals(listOf(Profile("007", "Ann Lee", "IT")), result.profiles)
    }

    @Test
    fun acceptsSemicolonsAndAnyColumnOrder() {
        val result = parsed("department;name;id;extra\nSales;Bob;12;x\n")
        assertEquals(listOf(Profile("12", "Bob", "Sales")), result.profiles)
    }

    @Test
    fun readsQuotedFields() {
        val result = parsed("id,name,department\n5,\"Smith, Jane\",\"R\"\"&\"\"D\"\n")
        assertEquals(Profile("5", "Smith, Jane", "R\"&\"D"), result.profiles.single())
    }

    @Test
    fun skipsBadLinesWithTheirNumbers() {
        val result = parsed("id,name,department\n1,A,X\n\n2,,X\n1,B,Y\n3,C,Z\n")
        assertEquals(listOf("1", "3"), result.profiles.map { it.id })
        assertEquals(listOf(SkippedLine(4, SkipReason.MISSING_FIELD), SkippedLine(5, SkipReason.DUPLICATE_ID, "1")), result.skipped)
    }

    @Test
    fun rejectsAFileWithoutTheColumns() {
        assertEquals(ImportResult.MissingHeader, Csv.parseProfiles("1,A,X\n2,B,Y\n"))
        assertEquals(ImportResult.MissingHeader, Csv.parseProfiles(""))
    }

    @Test
    fun exportKeepsMillisAndAddsReadableDateAndTime() {
        val record = Attendance(id = 7, employeeId = "007", employeeName = "Smith, Jane", employeeDepartment = "IT", clocking = Clocking.OUT, timestamp = 1_764_604_800_000L)
        val moved = record.copy(id = 8, clockShift = -62 * 60_000L)
        val lines = Csv.exportAttendance(listOf(record, moved), TimeZone.getTimeZone("UTC")).removePrefix(Csv.BOM.toString()).split("\r\n")
        assertEquals("attendance ID,employee ID,name,department,clocking,timestamp,date,time,clock changed (minutes)", lines[0])
        assertEquals("7,007,\"Smith, Jane\",IT,OUT,1764604800000,2025-12-01,16:00:00,", lines[1])
        assertEquals("8,007,\"Smith, Jane\",IT,OUT,1764604800000,2025-12-01,16:00:00,-62", lines[2])
    }
}
