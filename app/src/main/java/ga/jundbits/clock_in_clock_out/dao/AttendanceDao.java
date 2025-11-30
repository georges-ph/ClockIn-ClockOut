package ga.jundbits.clock_in_clock_out.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import ga.jundbits.clock_in_clock_out.models.Attendance;
import ga.jundbits.clock_in_clock_out.models.ProfileAttendance;

@Dao
public interface AttendanceDao {

    @Transaction
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    List<ProfileAttendance> getAll();

    @Insert
    void insert(Attendance attendance);

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeID ORDER BY timestamp DESC LIMIT 1")
    Attendance getLast(int employeeID);

}
