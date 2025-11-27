package ga.jundbits.clock_in_clock_out.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import ga.jundbits.clock_in_clock_out.models.Attendance;

@Dao
public interface AttendanceDao {

    @Insert
    void insert(Attendance attendance);

}
