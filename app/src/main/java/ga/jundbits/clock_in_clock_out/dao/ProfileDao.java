package ga.jundbits.clock_in_clock_out.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ga.jundbits.clock_in_clock_out.models.Profile;

@Dao
public interface ProfileDao {

    @Query("SELECT * FROM profile ORDER BY id ASC")
    List<Profile> getAll();

    @Query("SELECT count(*) FROM profile")
    int count();

    @Insert
    void insertAll(List<Profile> profiles);

    @Query("DELETE FROM profile")
    void deleteAll();

}
