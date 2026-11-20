package ga.jundbits.clock_in_clock_out.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM Profile ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Profile>>

    @Query("SELECT COUNT(*) FROM Profile")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM Profile")
    suspend fun count(): Int

    @Query("SELECT * FROM Profile WHERE id = :id")
    suspend fun get(id: String): Profile?

    @Query("DELETE FROM Profile")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(profiles: List<Profile>)

    /** The import replaces the whole list, so removed people are refused at the gate. */
    @Transaction
    suspend fun replaceAll(profiles: List<Profile>) {
        deleteAll()
        insertAll(profiles)
    }
}

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM Attendance ORDER BY timestamp DESC, id DESC")
    fun observeAll(): Flow<List<Attendance>>

    @Query("SELECT * FROM Attendance ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<Attendance?>

    @Query("SELECT COUNT(*) FROM Attendance WHERE timestamp >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT * FROM Attendance ORDER BY id ASC")
    suspend fun getAllForExport(): List<Attendance>

    @Query("SELECT * FROM Attendance WHERE employeeId = :employeeId ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun lastFor(employeeId: String): Attendance?

    @Insert
    suspend fun insert(attendance: Attendance)

    @Delete
    suspend fun delete(attendance: Attendance)
}
