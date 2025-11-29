package ga.jundbits.clock_in_clock_out.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import ga.jundbits.clock_in_clock_out.models.Attendance;

@Dao
public interface AttendanceDao {

    @Insert
    void insert(Attendance attendance);

    @Query("SELECT * FROM attendances WHERE employeeId = :employeeID ORDER BY timestamp DESC LIMIT 1")
    Attendance getLast(int employeeID);

}
