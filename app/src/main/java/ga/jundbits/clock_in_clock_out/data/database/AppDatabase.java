package ga.jundbits.clock_in_clock_out.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ga.jundbits.clock_in_clock_out.data.dao.AttendanceDao;
import ga.jundbits.clock_in_clock_out.data.dao.ProfileDao;
import ga.jundbits.clock_in_clock_out.data.entity.Attendance;
import ga.jundbits.clock_in_clock_out.data.entity.Profile;

@Database(entities = {Profile.class, Attendance.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase database;

    public abstract ProfileDao profileDao();
    public abstract AttendanceDao attendanceDao();

    public static AppDatabase getInstance(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, "clockin_clockout").build();
        }
        return database;
    }
}
