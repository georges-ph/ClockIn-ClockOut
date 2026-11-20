package ga.jundbits.clock_in_clock_out.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class SkipReason { MISSING_FIELD, DUPLICATE_ID }

data class SkippedLine(val line: Int, val reason: SkipReason, val id: String = "")

sealed interface ImportResult {
    /** The first line does not name the id, name and department columns. */
    data object MissingHeader : ImportResult
    data class Parsed(val profiles: List<Profile>, val skipped: List<SkippedLine>) : ImportResult
}

object Csv {

    /** Unicode byte order mark, which Excel writes at the start of "CSV UTF-8" files. */
    internal val BOM: Char = 0xFEFF.toChar()

    /**
     * Reads the employee list. Column order and letter case don't matter, extra
     * columns are ignored, and both comma and semicolon files work, since
     * spreadsheet apps in many locales save with semicolons.
     */
    fun parseProfiles(text: String): ImportResult {
        val clean = text.removePrefix(BOM.toString())
        val firstLine = clean.lineSequence().firstOrNull { it.isNotBlank() } ?: return ImportResult.MissingHeader
        val delimiter = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val records = parseRecords(clean, delimiter).filterNot { (_, fields) -> fields.all { it.isBlank() } }
        if (records.isEmpty()) return ImportResult.MissingHeader

        val header = records.first().second.map { it.trim().lowercase(Locale.ROOT) }
        val idColumn = header.indexOf("id")
        val nameColumn = header.indexOf("name")
        val departmentColumn = header.indexOf("department")
        if (idColumn < 0 || nameColumn < 0 || departmentColumn < 0) return ImportResult.MissingHeader

        val profiles = LinkedHashMap<String, Profile>()
        val skipped = mutableListOf<SkippedLine>()
        for ((line, fields) in records.drop(1)) {
            val id = fields.getOrNull(idColumn)?.trim().orEmpty()
            val name = fields.getOrNull(nameColumn)?.trim().orEmpty()
            val department = fields.getOrNull(departmentColumn)?.trim().orEmpty()
            when {
                id.isEmpty() || name.isEmpty() || department.isEmpty() -> skipped += SkippedLine(line, SkipReason.MISSING_FIELD)
                id in profiles -> skipped += SkippedLine(line, SkipReason.DUPLICATE_ID, id)
                else -> profiles[id] = Profile(id, name, department)
            }
        }
        return ImportResult.Parsed(profiles.values.toList(), skipped)
    }

    /**
     * Splits RFC 4180 text into records, each paired with the line number it starts
     * on. Quoted fields may hold the delimiter, doubled quotes and line breaks.
     */
    internal fun parseRecords(text: String, delimiter: Char): List<Pair<Int, List<String>>> {
        val records = mutableListOf<Pair<Int, List<String>>>()
        var fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var line = 1
        var recordLine = 1
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                when {
                    c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> { if (c == '\n') line++; field.append(c) }
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    delimiter -> { fields += field.toString(); field.clear() }
                    '\r' -> Unit
                    '\n' -> {
                        fields += field.toString(); field.clear()
                        records += recordLine to fields
                        fields = mutableListOf()
                        line++
                        recordLine = line
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) {
            fields += field.toString()
            records += recordLine to fields
        }
        return records
    }

    /**
     * Writes attendance for HR. The first six columns match app 2.0's export, with
     * the raw timestamp in milliseconds; date and time are added in the phone's
     * time zone. The last column says how many minutes the phone's clock was moved
     * before a record (negative is back), and is empty when it wasn't. The byte
     * order mark lets Excel read non-Latin names.
     */
    fun exportAttendance(records: List<Attendance>, timeZone: TimeZone = TimeZone.getDefault()): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { this.timeZone = timeZone }
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).apply { this.timeZone = timeZone }
        val builder = StringBuilder()
        builder.append(BOM)
        builder.append("attendance ID,employee ID,name,department,clocking,timestamp,date,time,clock changed (minutes)\r\n")
        for (record in records) {
            val stamp = Date(record.timestamp)
            listOf(
                record.id.toString(),
                record.employeeId,
                record.employeeName,
                record.employeeDepartment,
                record.clocking.name,
                record.timestamp.toString(),
                date.format(stamp),
                time.format(stamp),
                if (record.clockShift == 0L) "" else Math.round(record.clockShift / 60_000.0).toString(),
            ).joinTo(builder, ",") { escape(it) }
            builder.append("\r\n")
        }
        return builder.toString()
    }

    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }) "\"" + value.replace("\"", "\"\"") + "\""
        else value
}
