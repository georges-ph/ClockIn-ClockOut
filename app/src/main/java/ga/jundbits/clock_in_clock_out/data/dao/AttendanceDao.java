package ga.jundbits.clock_in_clock_out.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import ga.jundbits.clock_in_clock_out.data.entity.Attendance;
import ga.jundbits.clock_in_clock_out.data.entity.ProfileAttendance;

@Dao
public interface AttendanceDao {

    @Transaction
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    List<ProfileAttendance> getAllByTimestamp();

    @Transaction
    @Query("SELECT * FROM attendance ORDER BY id DESC")
    List<ProfileAttendance> getAllById();

    @Insert
    void insert(Attendance attendance);

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeID ORDER BY timestamp DESC LIMIT 1")
    Attendance getLast(int employeeID);

}
