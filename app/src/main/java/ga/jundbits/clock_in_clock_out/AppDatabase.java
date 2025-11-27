package ga.jundbits.clock_in_clock_out;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import ga.jundbits.clock_in_clock_out.dao.ProfileDao;
import ga.jundbits.clock_in_clock_out.models.Profile;

@Database(entities = {Profile.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase database;

    public abstract ProfileDao profileDao();

    public static AppDatabase getInstance(Context context) {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, "clockin_clockout").build();
        }
        return database;
    }
}
