package ga.jundbits.clock_in_clock_out.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Clocking { IN, OUT }

/** An employee from the imported list. Only the gate phone has these. */
@Entity(tableName = "Profile")
data class Profile(
    @PrimaryKey val id: String,
    val name: String,
    val department: String,
)

/**
 * One clock-in or clock-out. The name and department are copied in at the
 * time of the scan, so the record keeps them after the person is removed
 * from a later import.
 */
@Entity(tableName = "Attendance", indices = [Index("employeeId")])
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: String,
    val employeeName: String,
    val employeeDepartment: String,
    val clocking: Clocking,
    val timestamp: Long,
    /** How far the phone's clock was moved since the previous record, in milliseconds; 0 when it wasn't. */
    @ColumnInfo(defaultValue = "0") val clockShift: Long = 0,
)
